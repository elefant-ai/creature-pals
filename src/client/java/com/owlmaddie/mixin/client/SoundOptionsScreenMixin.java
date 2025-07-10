package com.owlmaddie.mixin.client;

import com.owlmaddie.player2.TTS;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import com.owlmaddie.mixin.client.SoundOptionsScreenAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends Screen {
    private SoundOptionsScreenMixin(Text title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    private void creaturechat$addTTSOption(CallbackInfo ci) {
        OptionListWidget list = ((SoundOptionsScreenAccessor)this).creaturechat$getOptionButtons();
        SimpleOption<Boolean> ttsOption = SimpleOption.ofBoolean("Creaturechat TTS", TTS.enabled, value -> TTS.enabled = value);
        list.addSingleOptionEntry(ttsOption);
    }

    private Text getTTSLabel() {
        return Text.literal("TTS: " + (TTS.enabled ? "ON" : "OFF"));
    }
}
