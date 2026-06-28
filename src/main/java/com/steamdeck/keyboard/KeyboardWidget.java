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
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean moving;

    public KeyboardWidget(int x, int y, int w, int h, InputTarget inputTarget, Runnable onClose) {
        super(x, y, w, h, Component.literal("Keyboard"));
        this.inputTarget = inputTarget;
        this.onClose = onClose;
        this.keys = new ArrayList<>();
        rebuildKeys();
    }

    private void rebuildKeys() {
        keys.clear();
        float uw = (float) getWidth() / KeyboardLayout.LAYOUT_WIDTH;
        float kh = (float) getHeight() / KeyboardLayout.ROWS.size();
        float relY = 0;
        for (List<KeyboardLayout.KeyDef> row : KeyboardLayout.ROWS) {
            float relX = 0;
            for (KeyboardLayout.KeyDef def : row) {
                float kw = def.width() * uw;
                keys.add(new KeyWidget((int) relX, (int) relY, (int) kw, (int) kh, def,
                    () -> onKeyPress(def)));
                relX += kw;
            }
            relY += kh;
        }
        updateAbsPositions();
    }

    /** Updates all key absolute positions based on current widget position */
    public void updateAbsPositions() {
        int px = getX();
        int py = getY();
        for (KeyWidget k : keys) {
            k.updateAbsolutePosition(px, py);
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateAbsPositions();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateAbsPositions();
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
            case TAB -> inputTarget.acceptSpecial(InputTarget.SpecialKey.TAB);
            case CLOSE -> onClose.run();
        }
    }

    public void setShifted(boolean s) { shifted = s; }
    private void setShiftLocked(boolean l) { shiftLocked = l; }
    public List<KeyWidget> getKeys() { return keys; }

    @Override
    protected void renderWidget(@NotNull GuiGraphics g, int mx, int my, float a) {
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0141414);
        g.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFF666666);
        for (KeyWidget k : keys) k.render(g, mx, my, a);
        for (KeyWidget k : keys) k.renderLabel(g, shifted);
    }

    public boolean startMoving(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            // check no key is under the mouse
            for (KeyWidget k : keys) {
                if (k.isMouseOverAbs(mouseX, mouseY)) return false;
            }
            moving = true;
            dragOffsetX = mouseX - getX();
            dragOffsetY = mouseY - getY();
            return true;
        }
        return false;
    }

    public void onDrag(double mouseX, double mouseY) {
        if (!moving) return;
        setX((int) (mouseX - dragOffsetX));
        setY((int) (mouseY - dragOffsetY));
    }

    public void stopMoving() {
        moving = false;
    }

    public boolean isMoving() { return moving; }

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
    @Override public boolean mouseClicked(double mx, double my, int button) {
        for (KeyWidget k : keys) { if (k.mouseClickedAbs(mx, my, button)) return true; }
        return startMoving(mx, my);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        for (KeyWidget k : keys) { k.mouseReleasedAbs(mx, my, button); }
        stopMoving();
        return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput o) {}
}
