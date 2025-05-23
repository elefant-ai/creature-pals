package com.owlmaddie.network;

import net.minecraft.util.Identifier;

public class NetworkingConstants {
    public static final Identifier PACKET_C2S_GREETING = Identifier.of("creaturechat", "packet_c2s_greeting");
    public static final Identifier PACKET_C2S_READ_NEXT = Identifier.of("creaturechat", "packet_c2s_read_next");
    public static final Identifier PACKET_C2S_SET_STATUS =  Identifier.of("creaturechat", "packet_c2s_set_status");
    public static final Identifier PACKET_C2S_OPEN_CHAT =  Identifier.of("creaturechat", "packet_c2s_open_chat");
    public static final Identifier PACKET_C2S_CLOSE_CHAT = Identifier.of("creaturechat", "packet_c2s_close_chat");
    public static final Identifier PACKET_C2S_SEND_CHAT =  Identifier.of("creaturechat", "packet_c2s_send_chat");
    public static final Identifier PACKET_S2C_LOGIN = Identifier.of("creaturechat", "packet_s2c_login");
    public static final Identifier PACKET_S2C_ENTITY_MESSAGE = Identifier.of("creaturechat", "packet_s2c_entity_message");
    public static final Identifier PACKET_S2C_WHITELIST =  Identifier.of("creaturechat", "packet_s2c_whitelist");
    public static final Identifier PACKET_S2C_PLAYER_MESSAGE = Identifier.of("creaturechat", "packet_s2c_player_message");
    public static final Identifier PACKET_S2C_PLAYER_STATUS = Identifier.of("creaturechat", "packet_s2c_player_status");

}
