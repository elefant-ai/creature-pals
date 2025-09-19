// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * The {@code ServerEntityFinder} class is used to find a specific LivingEntity
 * by UUID, since
 * there is not a built-in method for this.
 */
public class ServerEntityFinder {
    public static LivingEntity getEntityByUUID(ServerLevel world, UUID uuid) {
        for (Entity entity : world.getAllEntities()) {
            if (entity.getUUID().equals(uuid) && entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }
        return null; // Entity not found
    }

    public static List<Entity> getCloseEntities(ServerLevel world, ServerPlayer player, double cutoff) {
        List<Entity> output = new ArrayList<>();
        for (Entity entity : world.getAllEntities()) {
            boolean shouldAdd = (entity instanceof Mob)
                    && !entity.isVehicle()
                    && entity.distanceTo(player) < cutoff;
            if (shouldAdd) {
                output.add(entity);
            }
        }
        return output;
    }
}