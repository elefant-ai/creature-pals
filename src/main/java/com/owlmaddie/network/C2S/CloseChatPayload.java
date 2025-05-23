package com.owlmaddie.network.C2S;

import com.owlmaddie.network.NetworkingConstants;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

public record CloseChatPayload(boolean nothing) implements CustomPayload {
    public static final CustomPayload.Id<CloseChatPayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_C2S_CLOSE_CHAT);
    public static final PacketCodec<RegistryByteBuf, CloseChatPayload> CODEC = PacketCodec.tuple(PacketCodecs.BOOL, CloseChatPayload::nothing, CloseChatPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
