package io.github.stumper66.villagerannouncer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerInfo {
    public VillagerInfo(final @NotNull LivingEntity entity){
        this.entity = entity;
    }

    private final LivingEntity entity;

    boolean isNormalVillager;
    boolean isWanderingTrader;
    boolean isAdult = true;
    Entity killerEntity;
    EntityDamageEvent damageEvent;
    EntityDamageEvent.DamageCause damageCause;

    public boolean hasProfession(){
        final Villager.Profession profession = getProfession();
        return profession != null &&
                profession != Villager.Profession.NONE &&
                profession != Villager.Profession.NITWIT;
    }

    public @Nullable Villager.Profession getProfession(){
        if (entity instanceof Villager villager)
            return villager.getProfession();
        else if (entity instanceof ZombieVillager)
            return ((ZombieVillager) entity).getVillagerProfession();
        else
            return null;
    }

    public int getVillagerLevel(){
        if (entity instanceof Villager villager)
            return villager.getVillagerLevel();
        else
            return 0;
    }

    public @Nullable Villager.Type getVillagerType(){
        if (entity instanceof Villager villager)
            return villager.getVillagerType();
        else if (entity instanceof ZombieVillager)
            return ((ZombieVillager) entity).getVillagerType();
        else
            return null;
    }

    public int getVillagerExperience(){
        if (entity instanceof Villager villager)
            return villager.getVillagerExperience();
        else
            return 0;
    }
}
