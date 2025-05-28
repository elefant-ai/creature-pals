package com.owlmaddie.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The {@code ServerEntityFinder} class is used to find a specific LivingEntity
 * by UUID, since
 * there is not a built-in method for this.
 */
public class ServerEntityFinder {
    public static LivingEntity getEntityByUUID(ServerWorld world, UUID uuid) {
        for (Entity entity : world.iterateEntities()) {
            if (entity.getUuid().equals(uuid) && entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }
        return null; // Entity not found
    }

    public static List<Entity> getCloseEntities(ServerWorld world, ServerPlayerEntity player, double cutoff) {
        List<Entity> output = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            boolean shouldAdd = (entity instanceof MobEntity)
                    && !entity.hasPassengers()
                    && entity.distanceTo(player) < cutoff;
            if (shouldAdd) {
                output.add(entity);
            }
        }
        return output;
    }

}