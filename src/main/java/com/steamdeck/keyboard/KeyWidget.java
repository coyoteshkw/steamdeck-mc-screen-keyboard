package com.steamdeck.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

/**
 * Single key widget. Coordinates are relative to the parent KeyboardWidget.
 * Rendering is done via renderRelative() which takes the parent's absolute position.
 */
public class KeyWidget extends AbstractWidget {
    private static final int COLOR_BG = 0xC0444444;
    private static final int COLOR_BG_HOVER = 0xC0666666;
    private static final int COLOR_BG_PRESSED = 0xC0888888;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final KeyboardLayout.KeyDef keyDef;
    private final Runnable onPress;
    private boolean pressed;

    // Absolute screen position set by parent during layout
    private int absX, absY;

    public KeyWidget(int x, int y, int width, int height, KeyboardLayout.KeyDef keyDef, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.keyDef = keyDef;
        this.onPress = onPress;
        this.absX = x;
        this.absY = y;
    }

    public KeyboardLayout.KeyType getKeyType() { return keyDef.keyType(); }
    public KeyboardLayout.KeyDef getKeyDef() { return keyDef; }

    /** Called by parent to update absolute position after drag */
    public void updateAbsolutePosition(int parentX, int parentY) {
        this.absX = parentX + getX();
        this.absY = parentY + getY();
    }

    public int getAbsX() { return absX; }
    public int getAbsY() { return absY; }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mx, int my, float a) {
        int bg = pressed ? COLOR_BG_PRESSED : (isMouseOverAbs(mx, my) ? COLOR_BG_HOVER : COLOR_BG);
        g.fill(absX + 1, absY + 1, absX + getWidth() - 1, absY + getHeight() - 1, bg);
        g.fill(absX + 1, absY + 1, absX + getWidth() - 1, absY + 2, 0x40FFFFFF);
        g.fill(absX + 1, absY + getHeight() - 2, absX + getWidth() - 1, absY + getHeight() - 1, 0x40000000);
    }

    public void renderLabel(GuiGraphics g, boolean shifted) {
        String label = switch (keyDef.keyType()) {
            case CHAR -> String.valueOf(shifted ? keyDef.shifted() : keyDef.normal());
            case SHIFT -> shifted ? "\u21EA" : "\u21E7";
            case CAPS -> "Caps";
            case CLEAR -> "\u2327";
            case SPACE -> "";
            case BACKSPACE -> "\u2190";
            case ENTER -> "\u21B5";
            case TAB -> "\u21E5";
            case ARROW_UP -> "\u25B2";
            case ARROW_DOWN -> "\u25BC";
            case ARROW_LEFT -> "\u25C0";
            case ARROW_RIGHT -> "\u25B6";
            case CLOSE -> "\u00D7";
        };
        int tw = Minecraft.getInstance().font.width(label);
        int tx = absX + (getWidth() - tw) / 2;
        int ty = absY + (getHeight() - 8) / 2 + (pressed ? 1 : 0);
        g.drawString(Minecraft.getInstance().font, label, tx, ty, COLOR_TEXT);
    }

    public boolean isMouseOverAbs(double mx, double my) {
        return mx >= absX && mx < absX + getWidth() && my >= absY && my < absY + getHeight();
    }

    public boolean mouseClickedAbs(double mx, double my, int button) {
        if (isMouseOverAbs(mx, my) && isActive()) {
            pressed = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            onPress.run();
            return true;
        }
        return false;
    }

    public boolean mouseReleasedAbs(double mx, double my, int button) {
        pressed = false;
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (isMouseOverAbs(mx, my) && isActive()) {
            pressed = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        pressed = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput o) {}
}
