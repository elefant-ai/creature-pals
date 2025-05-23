package com.owlmaddie.network.S2C;

import com.owlmaddie.network.NetworkingConstants;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

public record LoginPayload(int sequence, int totalPackets, byte[] chunk) implements CustomPayload {
    public static final CustomPayload.Id<LoginPayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_S2C_LOGIN);
    public static final PacketCodec<RegistryByteBuf, LoginPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, LoginPayload::sequence,
            PacketCodecs.INTEGER, LoginPayload::totalPackets,
            PacketCodecs.BYTE_ARRAY, LoginPayload::chunk,
            LoginPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
