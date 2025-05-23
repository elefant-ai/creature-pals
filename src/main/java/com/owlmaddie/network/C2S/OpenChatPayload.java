package com.owlmaddie.network.C2S;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record OpenChatPayload(UUID entityId) implements CustomPayload {
    public static final CustomPayload.Id<OpenChatPayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_C2S_OPEN_CHAT);
    public static final PacketCodec<RegistryByteBuf, OpenChatPayload> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC, OpenChatPayload::entityId, OpenChatPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
