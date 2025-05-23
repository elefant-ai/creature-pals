package com.owlmaddie.network.S2C;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record PlayerStatusPayload(UUID senderId, boolean isChatOpen) implements CustomPayload {
    public static final CustomPayload.Id<PlayerStatusPayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_S2C_PLAYER_STATUS);
    public static final PacketCodec<RegistryByteBuf, PlayerStatusPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PlayerStatusPayload::senderId,
            PacketCodecs.BOOL, PlayerStatusPayload::isChatOpen,
            PlayerStatusPayload::new
    );



    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
