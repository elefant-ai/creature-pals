package com.owlmaddie.mixin.client;

import com.owlmaddie.screen.CreatureChatSettingsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptionsScreen.class)
public abstract class GameOptionsScreenMixin extends Screen {
    private GameOptionsScreenMixin(Text title) { super(title); }

    @Inject(method = "initFooter", at = @At("TAIL"))
    private void creaturechat$addSettingsButton(CallbackInfo ci) {
        ThreePartsLayoutWidget layout = ((GameOptionsScreen)(Object)this).layout;
        ButtonWidget widget = ButtonWidget.builder(Text.literal("CreatureChat"), button ->
                this.client.setScreen(new CreatureChatSettingsScreen((Screen)(Object)this)))
            .width(150)
            .build();
        layout.addFooter(widget);
    }
}
