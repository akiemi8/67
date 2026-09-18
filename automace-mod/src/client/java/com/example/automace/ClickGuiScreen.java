package com.example.automace;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class ClickGuiScreen extends Screen {

    private FallDistanceSlider fallSlider;

    public ClickGuiScreen() {
        super(Text.literal("Auto Mace Settings"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = 30;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Module: " + (Config.enabled ? "§aON" : "§cOFF")),
                b -> {
                    Config.enabled = !Config.enabled;
                    b.setMessage(Text.literal("Module: " + (Config.enabled ? "§aON" : "§cOFF")));
                    Config.save();
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 26;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("1-Tick Stun Slam: " + (Config.oneTickStunSlam ? "§aON" : "§cOFF")),
                b -> {
                    Config.oneTickStunSlam = !Config.oneTickStunSlam;
                    b.setMessage(Text.literal("1-Tick Stun Slam: " + (Config.oneTickStunSlam ? "§aON" : "§cOFF")));
                    Config.save();
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 26;

        fallSlider = new FallDistanceSlider(centerX - 100, y, 200, 20,
                Text.literal("Min Fall Distance: " + String.format("%.1f", Config.minFallDistance)),
                (Config.minFallDistance - 0.0f) / 8.0f);
        addDrawableChild(fallSlider);
        y += 30;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Only vs Players: " + (Config.onlyVsPlayers ? "§aON" : "§cOFF")),
                b -> {
                    Config.onlyVsPlayers = !Config.onlyVsPlayers;
                    b.setMessage(Text.literal("Only vs Players: " + (Config.onlyVsPlayers ? "§aON" : "§cOFF")));
                    Config.save();
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 26;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Restore Slot: " + (Config.restoreSlot ? "§aON" : "§cOFF")),
                b -> {
                    Config.restoreSlot = !Config.restoreSlot;
                    b.setMessage(Text.literal("Restore Slot: " + (Config.restoreSlot ? "§aON" : "§cOFF")));
                    Config.save();
                }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 32;

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(centerX - 50, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class FallDistanceSlider extends SliderWidget {
        public FallDistanceSlider(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
        }

        @Override
        protected void updateMessage() {
            float value = (float) (this.value * 8.0);
            Config.minFallDistance = MathHelper.floor(value * 10.0f) / 10.0f;
            setMessage(Text.literal("Min Fall Distance: " + String.format("%.1f", Config.minFallDistance)));
        }

        @Override
        protected void applyValue() {
            float value = (float) (this.value * 8.0);
            Config.minFallDistance = MathHelper.floor(value * 10.0f) / 10.0f;
            Config.save();
        }
    }
}
