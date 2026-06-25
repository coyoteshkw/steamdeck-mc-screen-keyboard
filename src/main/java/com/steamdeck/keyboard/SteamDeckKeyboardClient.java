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
import org.lwjgl.glfw.GLFW;

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
        // Use UNIVERSAL so the key is never consumed by GUI text fields.
        // We handle the "should this key actually toggle?" logic ourselves.
        toggleKeyMapping = new KeyMapping(
            "key.steamdeckkeyboard.toggle",
            net.neoforged.neoforge.client.settings.KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            KeyboardConfig.TOGGLE_KEY_CODE.get(),
            "category.steamdeckkeyboard"
        );
        event.register(toggleKeyMapping);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        boolean down = toggleKeyMapping.isDown();
        if (down && !wasToggleKeyDown) {
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

        // Case 2: No screen — nothing to toggle
        if (mc.screen == null) return;

        // Case 3: KeyboardScreen itself — safety guard (shouldn't happen, but belt and suspenders)
        if (mc.screen instanceof KeyboardScreen) return;

        // Case 4: An EditBox has focus — let the key pass through as normal text input.
        // Don't toggle the keyboard; the player is typing.
        if (hasFocusedEditBox(mc.screen)) return;

        // Case 5: GUI is open but no EditBox has focus (inventory, pause menu, etc.)
        // Open the keyboard targeting the first available EditBox on the screen.
        EditBox editBox = KeyboardInputHandler.findFocusedEditBox(mc.screen);
        if (editBox != null) {
            InputTarget target = KeyboardInputHandler.createInputTarget(editBox);
            KeyboardScreen.open(mc.screen, target);
        }
    }

    private static boolean hasFocusedEditBox(net.minecraft.client.gui.screens.Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof EditBox editBox && editBox.isFocused()) {
                return true;
            }
        }
        return false;
    }
}
