package com.owlmaddie.utils;

import net.minecraft.server.level.ServerPlayer;

import java.awt.*;

public class GetPlayerName {
    public static String getName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }
}
