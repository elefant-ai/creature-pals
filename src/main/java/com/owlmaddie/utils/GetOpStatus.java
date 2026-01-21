package com.owlmaddie.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class GetOpStatus {
    public static boolean isOp(PlayerList playerList, ServerPlayer player) {
        return playerList.isOp(player.getGameProfile());
    }
}
