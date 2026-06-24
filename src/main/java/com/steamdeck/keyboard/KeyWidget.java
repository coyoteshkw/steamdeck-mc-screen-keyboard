package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class KeyWidget extends AbstractWidget {
    private static final int COLOR_BG = 0xC0444444;
    private static final int COLOR_BG_HOVER = 0xC0666666;
    private static final int COLOR_BG_PRESSED = 0xC0888888;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final KeyboardLayout.KeyDef keyDef;
    private final Runnable onPress;
    private boolean pressed;

    public KeyWidget(int x, int y, int width, int height, KeyboardLayout.KeyDef keyDef, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.keyDef = keyDef;
        this.onPress = onPress;
    }

    public KeyboardLayout.KeyType getKeyType() { return keyDef.keyType(); }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float a) {
        int bg = pressed ? COLOR_BG_PRESSED : (isHovered() ? COLOR_BG_HOVER : COLOR_BG);
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, bg);
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + 2, 0x40FFFFFF);
        g.fill(getX() + 1, getY() + getHeight() - 2, getX() + getWidth() - 1, getY() + getHeight() - 1, 0x40000000);
    }

    public void renderLabel(GuiGraphicsExtractor g, boolean shifted) {
        String label = switch (keyDef.keyType()) {
            case CHAR -> String.valueOf(shifted ? keyDef.shifted() : keyDef.normal());
            case SHIFT -> shifted ? "\u21EA" : "\u21E7";
            case SPACE -> "";
            case BACKSPACE -> "\u2190";
            case ENTER -> "\u21B5";
            case CLOSE -> "\u00D7";
        };
        int tw = Minecraft.getInstance().font.width(label);
        int tx = getX() + (getWidth() - tw) / 2;
        int ty = getY() + (getHeight() - 8) / 2 + (pressed ? 1 : 0);
        g.text(Minecraft.getInstance().font, label, tx, ty, COLOR_TEXT);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent ev, boolean dbl) {
        if (isMouseOver(ev.x(), ev.y()) && isActive()) { pressed = true; onPress.run(); return true; }
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent ev) { pressed = false; return super.mouseReleased(ev); }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput o) {}
}
