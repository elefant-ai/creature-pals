package com.owlmaddie.screen;

import com.owlmaddie.player2.TTS;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import net.minecraft.text.Text;

public class CreatureChatSettingsScreen extends Screen {
    private final Screen parent;
    private ButtonWidget ttsButton;

    public CreatureChatSettingsScreen(Screen parent) {
        super(Text.literal("CreatureChat Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 10;
        this.ttsButton = ButtonWidget.builder(getTTSLabel(), this::toggleTTS)
            .size(150, 20)
            .position(centerX - 75, y)
            .build();
        this.addDrawableChild(ttsButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.client.setScreen(parent))
            .size(150, 20)
            .position(centerX - 75, y + 24)
            .build());
    }

    private void toggleTTS(ButtonWidget button) {
        TTS.enabled = !TTS.enabled;
        button.setMessage(getTTSLabel());
    }

    private Text getTTSLabel() {
        return Text.literal("TTS: " + (TTS.enabled ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}
