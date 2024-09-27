package io.github.stumper66.villagerannouncer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
            Log.war("Messages section in config.yml is null");
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

        final StringReplacer location = new StringReplacer(messages.getString("location", "&r( &6XYZ: %x% %y% %z%, &r&ein &r&a%world-name%&r)"));
        final Location loc = entity.getLocation();
        location.replaceIfExistsInt("%x%", loc::getBlockX);
        location.replaceIfExistsInt("%y%", loc::getBlockY);
        location.replaceIfExistsInt("%z%", loc::getBlockZ);
        location.replaceIfExists("%world-name%", () -> Objects.requireNonNull(loc.getWorld()).getName());
        location.replaceIfExists("%world-type%", () -> Objects.requireNonNull(loc.getWorld()).getEnvironment().name());

        String messageTemplate;
        if (wasInfected && info.hasProfession())
            messageTemplate = messages.getString("villager-infection-with-profession", "&eA %villager% has been infected! Profession: %villager-profession%, level: %villager-level%%location%");
        else if (wasInfected && !info.hasProfession())
            messageTemplate = messages.getString("villager-infection", "&eA %villager% has been infected! %location%");
        else if (info.killerEntity != null)
            messageTemplate = messages.getString("death-by-entity", "&eA %villager% was killed by %entity% %location%");
        else if (info.damageCause != null)
            messageTemplate = messages.getString("death-by-misc", "&eA %villager% died by %death-cause% %location%");
        else if (info.hasProfession())
            messageTemplate = messages.getString("villager-infection-with-profession", "&eA %villager% has been infected by %entity%! Profession: %villager-profession%, level: %villager-level% %location%");
        else // no infection regular death
            messageTemplate = messages.getString("villager-infection", "&eA %villager% has been infected by %entity%! %location%");

        final StringReplacer mainMessage = new StringReplacer(messageTemplate);
        mainMessage.replaceIfExists("%location%", () -> location.text);
        mainMessage.replaceIfExists("%villager%", () -> villager);
        mainMessage.replaceIfExists("%death-cause%", () -> info.damageCause != null ? capitalize(info.damageCause.name()) : "");
        mainMessage.replaceIfExists("%entity%", () -> {
            if (info.killerEntity == null) return "";
            if (info.killerEntity instanceof Player player) return player.getName();
            return info.killerEntity.getCustomName() != null ? info.killerEntity.getCustomName() : capitalize(info.killerEntity.getType().name());
        });
        mainMessage.replaceIfExists("%villager-profession%", () -> info.hasProfession() ? capitalize(info.getProfession().name()) : "");
        mainMessage.replaceIfExists("%villager-level%", () -> String.valueOf(info.getVillagerLevel()));
        mainMessage.replaceIfExists("%villager-experience%", () -> String.valueOf(info.getVillagerExperience()));
        mainMessage.replaceIfExists("%villager-type%", () -> capitalize(info.getVillagerType().name()));

        runBroadcast(mainMessage.text);
    }

    private @NotNull String capitalize(@NotNull final String str) {
        final StringBuilder builder = new StringBuilder();
        final String[] words = str.toLowerCase().split(" "); // each word separated from str
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            if (word.isEmpty()) {
                continue;
            }

            builder.append(String.valueOf(word.charAt(0)).toUpperCase()); // capitalize first letter
            if (word.length() > 1) {
                builder.append(word.substring(1)); // append the rest of the word
            }

            // if there is another word to capitalize, then add a space
            if (i < words.length - 1) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    private void runBroadcast(final String text) {
        final VillagerAnnouncer main = VillagerAnnouncer.getInstance();

        final List<String> allowedWorlds = main.config.getStringList("broadcast-worlds");
        if (!allowedWorlds.isEmpty()) {
            final String temp = main.config.getString("broadcast-worlds");
            if (temp != null && !temp.isEmpty())
                allowedWorlds.add(temp);
        }
        final String permissionName = "villagerannouncer.receive-broadcasts";
        final int maxRadius = main.config.getInt("max-broadcast-radius");
        final boolean requiresPermissions = main.config.getBoolean("players-require-premissions");
        final boolean useBroadcast = (allowedWorlds.isEmpty() || allowedWorlds.contains("*"))
                && !requiresPermissions && !main.onlyBroadcastIfTradedWith && maxRadius <= 0;
        final String message = MessageUtils.colorizeAll(text);

        if (useBroadcast) {
            Bukkit.broadcastMessage(message);
            if (main.discordSRVManager.getIsInstalled())
                main.discordSRVManager.sendMessage(message);
            if (!main.playSound) return;
        }
        else if (main.config.getBoolean("log-messages-to-console"))
            Log.inf(message);

        if (main.onlyBroadcastIfTradedWith){
            if (entity.getPersistentDataContainer().has(main.keyTraders, PersistentDataType.STRING))
                villagerTradedIds = entity.getPersistentDataContainer().get(main.keyTraders, PersistentDataType.STRING);
            else
                villagerTradedIds = "";
        }

        for (Player player : Bukkit.getOnlinePlayers()){
            if (main.onlyBroadcastIfTradedWith && !hadPlayerTradedWith(player)) continue;
            if (!checkWorldPermissions(player, allowedWorlds)) continue;
            if (requiresPermissions && !player.hasPermission(permissionName)) continue;
            if (maxRadius > 0 && !checkPlayerRadius(player, maxRadius)) continue;

            if (!useBroadcast) player.sendMessage(message);

            if (main.playSound && main.soundToPlay != null)
                player.playSound(player.getLocation(), main.soundToPlay, 1f, 1f);
        }
    }

    private boolean hadPlayerTradedWith(final @NotNull Player player){
        if (villagerTradedIds.isEmpty()) return false;
        return villagerTradedIds.contains(player.getUniqueId().toString());
    }

    private boolean checkPlayerRadius(final @NotNull Player player, final int maxRadius){
        if (entity.getWorld() != player.getWorld()) return false;

        final double distance = player.getLocation().distance(entity.getLocation());
        final int distanceInt = (int)Math.ceil(distance);
        return distanceInt <= maxRadius;
    }

    private boolean checkWorldPermissions(final @NotNull Player player, final @NotNull List<String> allowedWorlds){
        final String currentWorld = player.getWorld().getName();

        for (final String world : allowedWorlds){
            if ("*".equals(world)) return true;
            if (currentWorld.equalsIgnoreCase(world)) return true;
        }

        return false;
    }
}
