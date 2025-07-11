package com.owlmaddie.components;

import com.mojang.serialization.Codec;
import com.owlmaddie.commands.CreaturePalsCommands;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;


public class Components {
    public static final ComponentType<String> ChatUUID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("creaturepals", "CCUUID"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );
    protected static void initialize() {
        CreaturePalsCommands.LOGGER.info("Registering creaturepals components");
        // Technically this method can stay empty, but some developers like to notify
        // the console, that certain parts of the mod have been successfully initialized


    }
}
