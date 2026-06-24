package com.steamdeck.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyboardWidget extends AbstractWidget implements ContainerEventHandler {
    private final List<KeyWidget> keys;
    private final InputTarget inputTarget;
    private final Runnable onClose;
    private boolean shifted;
    private boolean shiftLocked;
    private @Nullable KeyWidget focusedKey;
    private boolean dragging;

    public KeyboardWidget(int x, int y, int w, int h, InputTarget inputTarget, Runnable onClose) {
        super(x, y, w, h, Component.literal("Keyboard"));
        this.inputTarget = inputTarget;
        this.onClose = onClose;
        this.keys = new ArrayList<>();
        float uw = (float) w / KeyboardLayout.LAYOUT_WIDTH;
        float kh = (float) h / KeyboardLayout.ROWS.size();
        float yPos = y;
        for (List<KeyboardLayout.KeyDef> row : KeyboardLayout.ROWS) {
            float xPos = x;
            for (KeyboardLayout.KeyDef def : row) {
                float kw = def.width() * uw;
                keys.add(new KeyWidget((int) xPos, (int) yPos, (int) kw, (int) kh, def,
                    () -> onKeyPress(def)));
                xPos += kw;
            }
            yPos += kh;
        }
    }

    private void onKeyPress(KeyboardLayout.KeyDef def) {
        switch (def.keyType()) {
            case CHAR -> {
                inputTarget.acceptChar(shifted ? def.shifted() : def.normal());
                if (shifted && !shiftLocked) setShifted(false);
            }
            case SPACE -> {
                inputTarget.acceptChar(' ');
                if (shifted && !shiftLocked) setShifted(false);
            }
            case BACKSPACE -> inputTarget.acceptSpecial(InputTarget.SpecialKey.BACKSPACE);
            case ENTER -> inputTarget.acceptSpecial(InputTarget.SpecialKey.ENTER);
            case SHIFT -> {
                if (!shifted) setShifted(true);
                else if (!shiftLocked) setShiftLocked(true);
                else { setShifted(false); setShiftLocked(false); }
            }
            case CLOSE -> onClose.run();
        }
    }

    public void setShifted(boolean s) { shifted = s; }
    private void setShiftLocked(boolean l) { shiftLocked = l; }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mx, int my, float a) {
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0141414);
        g.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF666666);
        for (KeyWidget k : keys) k.render(g, mx, my, a);
        for (KeyWidget k : keys) k.renderLabel(g, shifted);
    }

    @Override public @NotNull List<KeyWidget> children() { return Collections.unmodifiableList(keys); }
    @Override public boolean isDragging() { return dragging; }
    @Override public void setDragging(boolean d) { dragging = d; }
    @Override public @Nullable KeyWidget getFocused() { return focusedKey; }
    @Override public void setFocused(@Nullable GuiEventListener l) {
        if (focusedKey != null) focusedKey.setFocused(false);
        if (l instanceof KeyWidget kw) { focusedKey = kw; kw.setFocused(true); }
        else focusedKey = null;
    }
    @Override public @Nullable net.minecraft.client.gui.ComponentPath nextFocusPath(FocusNavigationEvent e) { return ContainerEventHandler.super.nextFocusPath(e); }
    @Override public boolean mouseClicked(double mx, double my, int button) { return ContainerEventHandler.super.mouseClicked(mx, my, button); }
    @Override public boolean mouseReleased(double mx, double my, int button) { return ContainerEventHandler.super.mouseReleased(mx, my, button); }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return ContainerEventHandler.super.mouseDragged(mx, my, button, dx, dy); }
    @Override protected void updateWidgetNarration(NarrationElementOutput o) {}
}
