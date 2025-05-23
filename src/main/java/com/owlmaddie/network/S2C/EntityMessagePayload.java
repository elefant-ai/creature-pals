package com.owlmaddie.network.S2C;

import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.network.NetworkingConstants;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.Map;
import java.util.UUID;

public record EntityMessagePayload(UUID entityID, String currentMessage, int currentLineNumber, String status, String sender, byte[] playerMap) implements CustomPayload {
    public static final CustomPayload.Id<EntityMessagePayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_S2C_ENTITY_MESSAGE);
    public static final PacketCodec<RegistryByteBuf, EntityMessagePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, EntityMessagePayload::entityID,
            PacketCodecs.STRING, EntityMessagePayload::currentMessage,
            PacketCodecs.INTEGER, EntityMessagePayload::currentLineNumber,
            PacketCodecs.STRING, EntityMessagePayload::status,
            PacketCodecs.STRING, EntityMessagePayload::sender,
            PacketCodecs.BYTE_ARRAY, EntityMessagePayload::playerMap,
            EntityMessagePayload::new
    );

    public static byte[] writePlayerDataMap(Map<UUID, PlayerData> map) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeInt(map.size()); // Write the size of the map
        for (Map.Entry<UUID, PlayerData> entry : map.entrySet()) {
            buffer.writeUuid(entry.getKey()); // Write the key (playerName)
            PlayerData data = entry.getValue();
            buffer.writeInt(data.friendship); // Write PlayerData field(s)
        }
        return buffer.array();
    }
    public static EntityMessagePayload make(UUID entityID, String currentMessage, int currentLineNumber, String status, String sender, Map<UUID, PlayerData> playerMap) {
        return new EntityMessagePayload(entityID, currentMessage, currentLineNumber, status, sender, writePlayerDataMap(playerMap));
    }
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}