package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.List;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    // When true, on next tick the keyboard will be opened on top of the current screen
    private static boolean pendingKeyboardOpen = false;

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
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        // Handle pending keyboard open (delayed from previous tick)
        if (pendingKeyboardOpen) {
            pendingKeyboardOpen = false;
            if (mc.screen != null && !(mc.screen instanceof KeyboardScreen) && !KeyboardScreen.isOpen(mc)) {
                openKeyboardOnScreen(mc, mc.screen);
            }
        }

        boolean down = toggleKeyMapping.consumeClick();

        if (down) {
            SteamDeckKeyboard.LOGGER.info("Toggle key pressed. screen={}, isKeyboardOpen={}",
                mc.screen != null ? mc.screen.getClass().getSimpleName() : "null",
                KeyboardScreen.isOpen(mc));
            handleTogglePress(mc);
        }
    }

    private void handleTogglePress(Minecraft mc) {
        // Keyboard is already open — close it
        if (KeyboardScreen.isOpen(mc)) {
            mc.setScreen(((KeyboardScreen) mc.screen).getBackgroundScreen());
            return;
        }

        // In-game, no screen — open chat + keyboard
        if (mc.screen == null) {
            // Set pending flag: keyboard will open on next tick after ChatScreen is active
            pendingKeyboardOpen = true;
            mc.setScreen(new ChatScreen(""));
            return;
        }

        // Safety
        if (mc.screen instanceof KeyboardScreen) return;

        // EditBox has focus — let K pass through as typing
        if (hasFocusedEditBox(mc.screen)) return;

        // Any other GUI — open keyboard directly
        openKeyboardOnScreen(mc, mc.screen);
    }

    private void openKeyboardOnScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        EditBox editBox = findAnyEditBox(screen);
        InputTarget target;
        if (editBox != null) {
            target = KeyboardInputHandler.createInputTarget(editBox);
        } else {
            target = new FallbackInputTarget();
        }
        SteamDeckKeyboard.LOGGER.info("Opening keyboard on {}", screen.getClass().getSimpleName());
        KeyboardScreen.open(screen, target);
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
     * Recursively find any EditBox on the screen, focused or not.
     */
    private static EditBox findAnyEditBox(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof ChatScreen chatScreen) {
            try {
                var field = ChatScreen.class.getDeclaredField("input");
                field.setAccessible(true);
                return (EditBox) field.get(chatScreen);
            } catch (Exception ignored) {}
        }
        return findEditBoxRecursive(screen.children());
    }

    private static EditBox findEditBoxRecursive(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children) {
        for (var child : children) {
            if (child instanceof EditBox editBox) {
                return editBox;
            }
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
                EditBox found = findEditBoxRecursive(container.children());
                if (found != null) return found;
            }
        }
        return null;
    }

    private static class FallbackInputTarget implements InputTarget {
        @Override
        public void acceptChar(char ch) {}

        @Override
        public void acceptSpecial(SpecialKey key) {}
    }
}
