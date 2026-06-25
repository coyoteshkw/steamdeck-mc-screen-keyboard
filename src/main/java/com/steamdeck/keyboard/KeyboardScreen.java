package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal overlay: just holds a KeyboardWidget and provides drag/close.
 * Instead of replacing mc.screen, this screen is pushed via setScreen,
 * but it delegates rendering and input to the backgroundScreen.
 * Closing simply restores backgroundScreen.
 */
public class KeyboardScreen extends Screen {
    private final Screen backgroundScreen;
    private final InputTarget inputTarget;
    private KeyboardWidget keyboardWidget;
    private double dragStartX, dragStartY;
    private int dragOffsetX, dragOffsetY;
    private boolean dragging;

    // Shared flag: when a KeyboardScreen is open, other instances know
    public static KeyboardScreen INSTANCE = null;

    public KeyboardScreen(Screen backgroundScreen, InputTarget inputTarget) {
        super(Component.literal("Keyboard Overlay"));
        this.backgroundScreen = backgroundScreen;
        this.inputTarget = inputTarget;
    }

    @Override
    protected void init() {
        INSTANCE = this;
        int kbdW = KeyboardConfig.KEYBOARD_WIDTH.get();
        int kbdH = KeyboardConfig.KEYBOARD_HEIGHT.get();
        if (kbdW <= 0) kbdW = (int) (width * 0.9);
        if (kbdH <= 0) kbdH = (int) (height * 0.3);
        int kbdX = KeyboardConfig.KEYBOARD_X.get();
        int kbdY = KeyboardConfig.KEYBOARD_Y.get();
        if (kbdX < 0) kbdX = (width - kbdW) / 2;
        if (kbdY < 0) kbdY = height - kbdH - 10;

        keyboardWidget = new KeyboardWidget(kbdX, kbdY, kbdW, kbdH, inputTarget, this::onClose);
        addRenderableWidget(keyboardWidget);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float a) {
        // Render background screen first, then keyboard on top
        backgroundScreen.render(g, mx, my, a);
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        super.render(g, mx, my, a);
        g.pose().popPose();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float a) {}

    @Override
    public void tick() { backgroundScreen.tick(); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_ESCAPE) {
            backgroundScreen.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return backgroundScreen.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (keyboardWidget != null && keyboardWidget.mouseClicked(mx, my, button)) return true;
        if (keyboardWidget != null && keyboardWidget.isMouseOver(mx, my)) {
            dragging = true;
            dragStartX = mx; dragStartY = my;
            dragOffsetX = keyboardWidget.getX(); dragOffsetY = keyboardWidget.getY();
            return true;
        }
        onClose();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && keyboardWidget != null) {
            int nx = dragOffsetX + (int)(mx - dragStartX);
            int ny = dragOffsetY + (int)(my - dragStartY);
            keyboardWidget.setX(Math.max(0, Math.min(nx, width - keyboardWidget.getWidth())));
            keyboardWidget.setY(Math.max(0, Math.min(ny, height - keyboardWidget.getHeight())));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        if (keyboardWidget != null) {
            KeyboardConfig.KEYBOARD_X.set(keyboardWidget.getX());
            KeyboardConfig.KEYBOARD_Y.set(keyboardWidget.getY());
            KeyboardConfig.KEYBOARD_WIDTH.set(keyboardWidget.getWidth());
            KeyboardConfig.KEYBOARD_HEIGHT.set(keyboardWidget.getHeight());
            KeyboardConfig.SPEC.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        INSTANCE = null;
        SteamDeckKeyboardClient.suppressAutoOpen = true;
        Minecraft.getInstance().setScreen(backgroundScreen);
    }

    @Override
    public void removed() {
        INSTANCE = null;
        super.removed();
    }

    public Screen getBackgroundScreen() { return backgroundScreen; }

    public static void open(Screen currentScreen, InputTarget target) {
        if (currentScreen instanceof KeyboardScreen) return;
        Minecraft.getInstance().setScreen(new KeyboardScreen(currentScreen, target));
    }

    public static boolean isOpen(Minecraft mc) { return mc.screen instanceof KeyboardScreen; }
}
