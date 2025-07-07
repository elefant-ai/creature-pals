package com.owlmaddie.mixin.client;

import com.owlmaddie.player2.TTS;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.text.Text;
import com.owlmaddie.mixin.client.GameOptionsScreenAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends Screen {
    private SoundOptionsScreenMixin(Text title) { super(title); }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void creaturechat$addTTSOption(CallbackInfo ci) {
        OptionListWidget list = ((GameOptionsScreenAccessor)this).creaturechat$getBody();
        ButtonWidget ttsButton = ButtonWidget.builder(getTTSLabel(), button -> {
            TTS.enabled = !TTS.enabled;
            button.setMessage(getTTSLabel());
        }).width(150).build();
        list.addWidgetEntry(ttsButton, null);
    }

    private Text getTTSLabel() {
        return Text.literal("TTS: " + (TTS.enabled ? "ON" : "OFF"));
    }
}
