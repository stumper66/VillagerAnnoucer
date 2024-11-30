package io.github.stumper66.villagerannouncer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class EventListeners implements Listener {
    public EventListeners(){
        instance = this;
        typesWeCareAbout = List.of(EntityType.VILLAGER, EntityType.ZOMBIE_VILLAGER, EntityType.WANDERING_TRADER);
        entitiesThatHurtVillagers = new LinkedHashMap<>();
        transformedVillagers = new HashSet<>();
    }

    private static EventListeners instance;
    private final List<EntityType> typesWeCareAbout;
    final Map<UUID, LivingEntity> entitiesThatHurtVillagers;
    final Set<UUID> transformedVillagers;
    private Instant lastEntryTime;

    public static EventListeners getInstance(){
        return instance;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeathEvent(final @NotNull EntityDeathEvent event){
        if (!VillagerAnnouncer.getInstance().isEnabled) return;
        if (!typesWeCareAbout.contains(event.getEntity().getType())) return;

        if (event.getEntity().getType() == EntityType.ZOMBIE_VILLAGER){
            final boolean wasPreviouslyNormalVillager = event.getEntity().getPersistentDataContainer().has(
                    VillagerAnnouncer.getInstance().keyWasVillager,
                    PersistentDataType.INTEGER
            );

            if (!wasPreviouslyNormalVillager) return;
        }

        if (event.getEntity().getType() == EntityType.WANDERING_TRADER){
            if (VillagerAnnouncer.getInstance().config.getBoolean("include-wandering-trader"))
                return;
        }

        // the transform event occurs after the death event
        // so we have to delay checking so the transform event can run first
        final VillagerDeath vd = new VillagerDeath(event.getEntity());
        Bukkit.getScheduler().runTaskLater(VillagerAnnouncer.getInstance(), vd::run, 20);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageEvent(final @NotNull EntityDamageByEntityEvent event){
        if (!VillagerAnnouncer.getInstance().isEnabled) return;
        if (!typesWeCareAbout.contains(event.getEntity().getType())) return;

        LivingEntity damager;
        if (event.getDamager() instanceof LivingEntity livingDamager)
            damager = livingDamager;
        else if (event.getDamager() instanceof Projectile projectileDamager)
            damager = (LivingEntity) projectileDamager.getShooter();
        else
            return;

        if (lastEntryTime != null){
            final long howLongMS = Duration.between(lastEntryTime, Instant.now()).toMillis();
            if (howLongMS >= 10000){
                entitiesThatHurtVillagers.clear();
                transformedVillagers.clear();
            }
        }

        lastEntryTime = Instant.now();
        entitiesThatHurtVillagers.put(event.getEntity().getUniqueId(), damager);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTransformEvent(final @NotNull EntityTransformEvent event){
        if (!VillagerAnnouncer.getInstance().isEnabled) return;
        if (event.getEntity().getType() != EntityType.VILLAGER) return;
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.INFECTION) return;

        for (final Entity entity : event.getTransformedEntities()){
            entity.getPersistentDataContainer().set(
                    VillagerAnnouncer.getInstance().keyWasVillager,
                    PersistentDataType.INTEGER,
                    1
            );
        }

        lastEntryTime = Instant.now();
        transformedVillagers.add(event.getEntity().getUniqueId());
        final VillagerDeath villagerDeath = new VillagerDeath((LivingEntity) event.getEntity());

        villagerDeath.wasInfected = true;
        villagerDeath.run();
    }
}
