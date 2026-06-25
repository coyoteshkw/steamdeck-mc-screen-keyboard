package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean wasToggleKeyDown = false;

    public SteamDeckKeyboardClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, KeyboardConfig.SPEC);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
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
        SteamDeckKeyboard.LOGGER.info("Registered toggle key with code: {}", KeyboardConfig.TOGGLE_KEY_CODE.get());
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        boolean down = toggleKeyMapping.isDown();

        if (down && !wasToggleKeyDown) {
            SteamDeckKeyboard.LOGGER.info("Toggle key pressed. screen={}, isKeyboardOpen={}",
                mc.screen != null ? mc.screen.getClass().getSimpleName() : "null",
                KeyboardScreen.isOpen(mc));
            handleTogglePress(mc);
        }
        wasToggleKeyDown = down;
    }

    private void handleTogglePress(Minecraft mc) {
        // Case 1: Keyboard is open — close it unconditionally
        if (KeyboardScreen.isOpen(mc)) {
            mc.setScreen(((KeyboardScreen) mc.screen).getBackgroundScreen());
            return;
        }

        // Case 2: No screen — open keyboard for chat input (player pressed K in-game)
        if (mc.screen == null) {
            // Open chat screen first, then keyboard on top
            mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
            // On next tick the ChatScreen will be open, keyboard will follow via auto-open
            return;
        }

        // Case 3: KeyboardScreen itself — safety guard
        if (mc.screen instanceof KeyboardScreen) return;

        // Case 4: An EditBox has focus — let the key pass through as normal text input
        if (hasFocusedEditBox(mc.screen)) return;

        // Case 5: GUI is open but no EditBox has focus (inventory, pause menu, etc.)
        // Find any EditBox on screen, or create a fallback target
        EditBox editBox = KeyboardInputHandler.findFocusedEditBox(mc.screen);
        InputTarget target;
        if (editBox != null) {
            target = KeyboardInputHandler.createInputTarget(editBox);
        } else {
            // Fallback: create an InputTarget backed by a dummy EditBox
            // This lets the keyboard appear even when no EditBox is visible.
            // Characters are buffered until the player focuses a real EditBox.
            target = new FallbackInputTarget();
        }
        KeyboardScreen.open(mc.screen, target);
    }

    private static boolean hasFocusedEditBox(net.minecraft.client.gui.screens.Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof EditBox editBox && editBox.isFocused()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fallback InputTarget used when no EditBox is available on the current screen.
     * Characters are discarded silently. The keyboard is visible but non-functional
     * until the user opens a screen with an actual EditBox.
     */
    private static class FallbackInputTarget implements InputTarget {
        @Override
        public void acceptChar(char ch) {
            // No target EditBox — silently discard
        }

        @Override
        public void acceptSpecial(SpecialKey key) {
            // No target — silently discard
        }
    }
}
