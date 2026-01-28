package io.github.stumper66.villagerannouncer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class VillagerDeath {
    public VillagerDeath(final @NotNull LivingEntity entity){
        this.entity = entity;
    }

    private final LivingEntity entity;
    private VillagerInfo info;
    public boolean wasInfected;
    private String villagerTradedIds = "";

    public void run(){
        final EventListeners eventListeners = EventListeners.getInstance();
        info = new VillagerInfo(entity);
        info.isNormalVillager = entity.getType() == EntityType.VILLAGER;
        info.isWanderingTrader = entity.getType() == EntityType.WANDERING_TRADER;

        if (!wasInfected && info.isNormalVillager && eventListeners.transformedVillagers.contains(entity.getUniqueId())){
            eventListeners.transformedVillagers.remove(entity.getUniqueId());
            return;
        }

        if (entity instanceof Ageable ageable && !ageable.isAdult())
            info.isAdult = false;

        info.killerEntity = entity.getKiller();
        if (info.killerEntity != null && info.killerEntity.getUniqueId() == entity.getUniqueId())
            info.killerEntity = null;

        if (info.killerEntity == null && eventListeners.entitiesThatHurtVillagers.containsKey(entity.getUniqueId()))
            info.killerEntity = eventListeners.entitiesThatHurtVillagers.get(entity.getUniqueId());

        info.damageEvent = entity.getLastDamageCause();

        if (!wasInfected && !(info.killerEntity instanceof Player)) {
            assert info.damageEvent != null;
            if ((info.damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || info.damageEvent.getCause()== EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                    && eventListeners.entitiesThatHurtVillagers.containsKey(entity.getUniqueId())){
                info.killerEntity = eventListeners.entitiesThatHurtVillagers.get(entity.getUniqueId());
            }
            else if (info.damageEvent.getEntity().getUniqueId() != entity.getUniqueId())
                info.killerEntity = info.damageEvent.getEntity();
            else
                info.damageCause = info.damageEvent.getCause();
        }

        formulateMessage();
    }

    private void formulateMessage(){
        final YamlConfiguration config = VillagerAnnouncer.getInstance().config;
        final ConfigurationSection messages = config.getConfigurationSection("messages");
        if (messages == null){
            return;
        }

        String villager;
        if (info.isNormalVillager && info.isAdult)
            villager = messages.getString("villager", "villager");
        else if (info.isNormalVillager && !info.isAdult)
            villager = messages.getString("baby-villager", "baby villager");
        else if (!info.isNormalVillager && info.isAdult)
            villager = messages.getString("zombie-villager", "zombie villager");
        else
            villager = messages.getString("baby-zombie-villager", "baby zombie villager");

        final String wanderingTrader = messages.getString("wandering-trader", "wandering trader");
        final Location loc = entity.getLocation();

        String messageTemplate;
        if (info.isWanderingTrader){
            if (info.killerEntity != null)
                messageTemplate = messages.getString("wandering-trader-death-by-entity");
            else
                messageTemplate = messages.getString("wandering-trader-death");
        }
        else {
            if (wasInfected && info.hasProfession())
                messageTemplate = messages.getString("villager-infection-with-profession");
            else if (wasInfected)
                messageTemplate = messages.getString("villager-infection");
            else if (info.killerEntity != null)
                messageTemplate = messages.getString("death-by-entity");
            else if (info.damageCause != null)
                messageTemplate = messages.getString("death-by-misc");
            else
                messageTemplate = messages.getString("villager-infection");
        }

        if (info.killerEntity == null && "disabled".equalsIgnoreCase(messageTemplate)) return;

        final StringReplacer mainMessage = new StringReplacer(messageTemplate);
        mainMessage.replaceIfExists("%villager%", () -> villager);
        mainMessage.replaceIfExists("%location%", () ->
                "( XYZ: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() +
                        ", in " + Objects.requireNonNull(loc.getWorld()).getName() + " )"
        );
        mainMessage.replaceIfExists("%death-cause%", () ->
                info.damageCause != null ? info.damageCause.name() : ""
        );
        mainMessage.replaceIfExists("%entity%", () -> {
            if (info.killerEntity == null) return "";
            if (info.killerEntity instanceof Player player) return player.getName();
            return info.killerEntity.getType().name();
        });

        runBroadcast(mainMessage.text);
    }

    private void runBroadcast(final String text) {
        final VillagerAnnouncer main = VillagerAnnouncer.getInstance();
        final Component comp = MiniMessage.miniMessage().deserialize(text);

        for (final Player player : Bukkit.getOnlinePlayers()){
            if (main.onlyBroadcastIfTradedWith && !hadPlayerTradedWith(player)) continue;
            if (main.config.getBoolean("players-require-premissions")
                    && !player.hasPermission("villagerannouncer.receive-broadcasts")) continue;

            // meechie toggle
            if (main.isMuted(player)) continue;

            main.adventure.player(player).sendMessage(comp);

            if (main.playSound){
                final SoundInfo soundInfo = info.isNormalVillager
                        ? main.soundsNormal
                        : main.soundsWanderingTrader;

                final Sound sound = soundInfo.getSoundToBePlayed();
                if (sound != null)
                    player.playSound(player.getLocation(), sound, 1f, 1f);
            }
        }
    }

    private boolean hadPlayerTradedWith(final @NotNull Player player){
        if (villagerTradedIds.isEmpty()) return false;
        return villagerTradedIds.contains(player.getUniqueId().toString());
    }
}
