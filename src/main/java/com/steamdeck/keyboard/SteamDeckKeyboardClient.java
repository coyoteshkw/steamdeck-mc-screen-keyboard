package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean pendingKeyboardOpen = false;
    static boolean suppressAutoOpen = false;

    public SteamDeckKeyboardClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, KeyboardConfig.SPEC);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onMouseReleased);
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

    private void onMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        if (!KeyboardConfig.AUTO_OPEN_OTHERS.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || KeyboardScreen.isOpen(mc) || mc.screen instanceof KeyboardScreen) return;
        if (suppressAutoOpen) return;

        // After any mouse click, check if an EditBox just got focused
        // Use a short delay to let MC process the focus change first
        pendingAutoOpenCheck = true;
    }

    private static boolean pendingAutoOpenCheck = false;

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        // One-tick delayed check after mouse release
        if (pendingAutoOpenCheck) {
            pendingAutoOpenCheck = false;
            if (mc.screen != null && !KeyboardScreen.isOpen(mc) && !(mc.screen instanceof KeyboardScreen) && !suppressAutoOpen) {
                if (KeyboardConfig.AUTO_OPEN_OTHERS.get()) {
                    EditBox focused = KeyboardInputHandler.findFocusedEditBox(mc.screen);
                    if (focused != null) {
                        SteamDeckKeyboard.LOGGER.info("Auto-open via mouse: found focused EditBox on {}", mc.screen.getClass().getSimpleName());
                        openKeyboardOnScreen(mc, mc.screen);
                    }
                }
            }
        }

        // Delayed open from K key
        if (pendingKeyboardOpen) {
            pendingKeyboardOpen = false;
            if (mc.screen != null && !(mc.screen instanceof KeyboardScreen) && !KeyboardScreen.isOpen(mc)) {
                openKeyboardOnScreen(mc, mc.screen);
            }
        }

        if (toggleKeyMapping.consumeClick()) {
            handleTogglePress(mc);
        }
    }

    private void handleTogglePress(Minecraft mc) {
        if (KeyboardScreen.isOpen(mc)) {
            mc.setScreen(((KeyboardScreen) mc.screen).getBackgroundScreen());
            suppressAutoOpen = true;
            return;
        }
        if (mc.screen == null) {
            pendingKeyboardOpen = true;
            mc.setScreen(new ChatScreen(""));
            return;
        }
        if (mc.screen instanceof KeyboardScreen) return;

        openKeyboardOnScreen(mc, mc.screen);
    }

    private void openKeyboardOnScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        KeyboardInputHandler.openKeyboardForScreen(screen);
    }
}
