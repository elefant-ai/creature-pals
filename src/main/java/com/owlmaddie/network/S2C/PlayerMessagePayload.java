package com.owlmaddie.network.S2C;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.List;
import java.util.UUID;

public record PlayerMessagePayload(UUID senderId, String senderName, String message, boolean fromMinecraftChat) implements CustomPayload {
    public static final CustomPayload.Id<PlayerMessagePayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_S2C_PLAYER_MESSAGE);
    public static final PacketCodec<RegistryByteBuf, PlayerMessagePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PlayerMessagePayload::senderId,
            PacketCodecs.STRING, PlayerMessagePayload::senderName,
            PacketCodecs.STRING, PlayerMessagePayload::message,
            PacketCodecs.BOOL, PlayerMessagePayload::fromMinecraftChat,
            PlayerMessagePayload::new
    );



    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
