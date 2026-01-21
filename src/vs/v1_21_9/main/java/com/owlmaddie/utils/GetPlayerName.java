package com.owlmaddie.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GetPlayerName {
    public static String getName(ServerPlayer player) {
        return player.getName().toString();
    }
}
