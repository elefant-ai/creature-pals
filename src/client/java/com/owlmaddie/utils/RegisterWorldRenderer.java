package com.owlmaddie.utils;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class RegisterWorldRenderer {

    public static void register(WorldRenderEvents.AfterEntities event) {
        WorldRenderEvents.AFTER_ENTITIES.register(event);
    }


}

