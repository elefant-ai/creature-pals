package com.owlmaddie.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.PlayerMessageManager;

/**
 * The {@code ClientEntityFinder} class is used to find a specific MobEntity by
 * UUID, since
 * there is not a built-in method for this. Also has a method for client
 * PlayerEntity lookup.
 */
public class ClientEntityFinder {
    public static MobEntity getEntityByUUID(ClientWorld world, UUID uuid) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUuid().equals(uuid) && entity instanceof MobEntity) {
                return (MobEntity) entity;
            }
        }
        return null; // Entity not found
    }

    public static PlayerEntity getPlayerEntityFromUUID(UUID uuid) {
        return MinecraftClient.getInstance().world.getPlayers().stream()
                .filter(player -> player.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets entity closest to player that has chat bubble open.
     */
    public static Optional<Entity> getClosestEntityToPlayerWithChatBubbleOpen() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        Optional<Entity> closest = BubbleRenderer.getRelevantEntities().stream()
                .filter(entity -> {
                    if (!(entity instanceof MobEntity))
                        return false;
                    EntityChatData chatData = ChatDataManager.getClientInstance()
                            .getOrCreateChatData(entity.getUuidAsString());
                    return chatData.status != ChatStatus.HIDDEN && chatData.status != ChatStatus.NONE;
                })
                .min(Comparator.comparingDouble(e -> e.getPos().distanceTo(player.getPos())));
        return closest;
    }
}
