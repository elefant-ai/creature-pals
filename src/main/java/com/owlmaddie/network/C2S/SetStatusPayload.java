package com.owlmaddie.network.C2S;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record SetStatusPayload(UUID entityId, String statusName) implements CustomPayload {
    public static final CustomPayload.Id<SetStatusPayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_C2S_SET_STATUS);
    public static final PacketCodec<RegistryByteBuf, SetStatusPayload> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC, SetStatusPayload::entityId, PacketCodecs.STRING, SetStatusPayload::statusName, SetStatusPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}