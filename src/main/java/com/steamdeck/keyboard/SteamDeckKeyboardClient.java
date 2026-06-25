package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean pendingKeyboardOpen = false;

    private static KeyboardWidget activeKeyboard = null;
    private static net.minecraft.client.gui.screens.Screen keyboardHostScreen = null;
    private static KeyboardInputHandler.SimpleInputTarget activeInputTarget = null;

    // Track mouse state for keyboard key clicks
    private static boolean mouseWasDown = false;

    public SteamDeckKeyboardClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, KeyboardConfig.SPEC);
        modEventBus.addListener(this::registerKeyMappings);
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
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            int btnX = screen.width - 70;
            int btnY = 5;
            event.addListener(
                net.minecraft.client.gui.components.Button.builder(
                    Component.literal("\u2328"),
                    btn -> openKeyboard(screen)
                ).pos(btnX, btnY).size(20, 20).build()
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

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        // Handle keyboard key clicks by polling mouse state
        if (activeKeyboard != null && keyboardHostScreen == mc.screen) {
            long window = mc.getWindow().getWindow();
            boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            if (leftDown && !mouseWasDown) {
                // Mouse just pressed
                double[] x = new double[1], y = new double[1];
                GLFW.glfwGetCursorPos(window, x, y);
                double mx = x[0] * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
                double my = y[0] * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

                if (activeKeyboard.isMouseOver(mx, my)) {
                    // Try each key
                    for (var key : activeKeyboard.getKeys()) {
                        if (key.isMouseOverAbs(mx, my)) {
                            key.mouseClickedAbs(mx, my, 0);
                            break;
                        }
                    }
                }
            }
            if (!leftDown && mouseWasDown) {
                // Mouse released
                for (var key : activeKeyboard.getKeys()) {
                    key.mouseReleasedAbs(0, 0, 0);
                }
            }
            mouseWasDown = leftDown;
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
        if (kbdY < 0) kbdY = screen.height - kbdH - 10;
        return new KeyboardWidget(kbdX, kbdY, kbdW, kbdH, target, SteamDeckKeyboardClient::closeKeyboard);
    }
}
