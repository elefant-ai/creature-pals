package com.owlmaddie.mixin.client;

import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundOptionsScreen.class)
public interface SoundOptionsScreenAccessor {
    @Accessor("optionButtons")
    OptionListWidget creaturechat$getOptionButtons();
}
