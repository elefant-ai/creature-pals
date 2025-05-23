package com.owlmaddie.network.C2S;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record GreetingPayload(UUID entityId, String userLanguage) implements CustomPayload {
    public static final CustomPayload.Id<GreetingPayload> ID = new CustomPayload.Id<>(NetworkingConstants.PACKET_C2S_GREETING);
    public static final PacketCodec<RegistryByteBuf, GreetingPayload> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC, GreetingPayload::entityId, PacketCodecs.STRING, GreetingPayload::userLanguage, GreetingPayload::new);
    // should you need to send more data, add the appropriate record parameters and change your codec:
    // public static final PacketCodec<RegistryByteBuf, BlockHighlightPayload> CODEC = PacketCodec.tuple(
    //         BlockPos.PACKET_CODEC, BlockHighlightPayload::blockPos,
    //         PacketCodecs.INTEGER, BlockHighlightPayload::myInt,
    //         Uuids.PACKET_CODEC, BlockHighlightPayload::myUuid,
    //         BlockHighlightPayload::new
    // );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}