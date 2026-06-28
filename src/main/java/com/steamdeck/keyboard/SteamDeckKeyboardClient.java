package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean pendingKeyboardOpen = false;

    private static KeyboardWidget activeKeyboard = null;
    private static net.minecraft.client.gui.screens.Screen keyboardHostScreen = null;
    private static KeyboardInputHandler.SimpleInputTarget activeInputTarget = null;

    public SteamDeckKeyboardClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, KeyboardConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, screen) -> new ConfigurationScreen(container, screen));
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onInputMouseButton);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onScreenInit);
        NeoForge.EVENT_BUS.addListener(this::onScreenRenderPost);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKeyMapping = new KeyMapping(
            "key.steamdeckkeyboard.toggle",
            net.neoforged.neoforge.client.settings.KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            KeyboardConfig.TOGGLE_KEY_CODE.get(),
            "category.steamdeckkeyboard"
        );
        event.register(toggleKeyMapping);
    }

    private void onScreenInit(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (screen instanceof InventoryScreen
            || screen instanceof CreativeModeInventoryScreen
            || screen instanceof AbstractContainerScreen) {
            int btnX = 45;
            int btnY = screen.height - 22;
            event.addListener(
                net.minecraft.client.gui.components.Button.builder(
                    Component.literal("⌨"),
                    btn -> openKeyboard(screen)
                ).pos(btnX, btnY).size(20, 20).build()
            );
            event.addListener(
                net.minecraft.client.gui.components.Button.builder(
                    Component.literal("⟲"),
                    btn -> {
                        KeyboardConfig.KEYBOARD_X.set(-1);
                        KeyboardConfig.KEYBOARD_Y.set(-1);
                        if (activeKeyboard != null) {
                            int w = activeKeyboard.getWidth();
                            int h = activeKeyboard.getHeight();
                            activeKeyboard.setX((screen.width - w) / 2);
                            activeKeyboard.setY(screen.height - h - 40);
                        }
                    }
                ).pos(btnX + 21, btnY).size(20, 20).build()
            );
        }
    }

    private void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (activeKeyboard != null && keyboardHostScreen == event.getScreen()) {
            var g = event.getGuiGraphics();
            g.pose().pushPose();
            g.pose().translate(0, 0, 500);
            activeKeyboard.render(g, (int)event.getMouseX(), (int)event.getMouseY(), event.getPartialTick());
            g.pose().popPose();
        }
    }

    /**
     * Intercept mouse clicks at the InputEvent level (before ScreenEvent),
     * with HIGHEST priority to run before JEI/EMI handlers.
     * Cancel the GLFW event so it never becomes a ScreenEvent.
     */
    private void onInputMouseButton(InputEvent.MouseButton.Pre event) {
        if (activeKeyboard == null || keyboardHostScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != keyboardHostScreen) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        if (!activeKeyboard.isMouseOver(mx, my)) return;

        event.setCanceled(true);

        if (event.getAction() == GLFW.GLFW_PRESS) {
            for (var key : activeKeyboard.getKeys()) {
                if (key.isMouseOverAbs(mx, my)) {
                    key.mouseClickedAbs(mx, my, 0);
                    return;
                }
            }
            // Clicked on empty area — start drag
            activeKeyboard.startMoving(mx, my);
        } else {
            for (var key : activeKeyboard.getKeys()) {
                key.mouseReleasedAbs(mx, my, 0);
            }
            if (activeKeyboard.isMoving()) {
                activeKeyboard.stopMoving();
                KeyboardConfig.KEYBOARD_X.set(activeKeyboard.getX());
                KeyboardConfig.KEYBOARD_Y.set(activeKeyboard.getY());
            }
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        // Handle reset position config toggle
        if (KeyboardConfig.RESET_POSITION.get()) {
            KeyboardConfig.KEYBOARD_X.set(-1);
            KeyboardConfig.KEYBOARD_Y.set(-1);
            KeyboardConfig.RESET_POSITION.set(false);
        }

        // Handle keyboard dragging
        if (activeKeyboard != null && activeKeyboard.isMoving()) {
            double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
            activeKeyboard.onDrag(mx, my);
        }

        if (pendingKeyboardOpen) {
            pendingKeyboardOpen = false;
            if (mc.screen != null) openKeyboard(mc.screen);
        }

        if (toggleKeyMapping.consumeClick()) {
            handleTogglePress(mc);
        }

        if (activeKeyboard != null && mc.screen != keyboardHostScreen) {
            closeKeyboard();
        }
    }

    private void handleTogglePress(Minecraft mc) {
        if (activeKeyboard != null) { closeKeyboard(); return; }
        if (mc.screen == null) { pendingKeyboardOpen = true; mc.setScreen(new ChatScreen("")); return; }
        openKeyboard(mc.screen);
    }

    private void openKeyboard(net.minecraft.client.gui.screens.Screen screen) {
        if (activeKeyboard != null) closeKeyboard();
        activeInputTarget = new KeyboardInputHandler.SimpleInputTarget(screen);
        activeKeyboard = createKeyboardWidget(screen, activeInputTarget);
        keyboardHostScreen = screen;
    }

    static void closeKeyboard() {
        activeKeyboard = null;
        keyboardHostScreen = null;
        activeInputTarget = null;
    }

    private KeyboardWidget createKeyboardWidget(net.minecraft.client.gui.screens.Screen screen, InputTarget target) {
        int kbdW = KeyboardConfig.KEYBOARD_WIDTH.get();
        int kbdH = KeyboardConfig.KEYBOARD_HEIGHT.get();
        if (kbdW <= 0) kbdW = (int) (screen.width * 0.9);
        if (kbdH <= 0) kbdH = (int) (screen.height * 0.3);
        int kbdX = KeyboardConfig.KEYBOARD_X.get();
        int kbdY = KeyboardConfig.KEYBOARD_Y.get();
        if (kbdX < 0) kbdX = (screen.width - kbdW) / 2;
        if (kbdY < 0) kbdY = screen.height - kbdH - 40;
        return new KeyboardWidget(kbdX, kbdY, kbdW, kbdH, target, SteamDeckKeyboardClient::closeKeyboard);
    }
}
