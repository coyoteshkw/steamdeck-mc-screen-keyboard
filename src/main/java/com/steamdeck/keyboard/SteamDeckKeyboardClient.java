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
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean pendingKeyboardOpen = false;
    private static EditBox lastFocusedEditBox = null;
    // Prevent re-open right after manual close
    static boolean suppressAutoOpen = false;

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

        // --- Auto-open: detect newly focused EditBox (only if config enables it) ---
        if (KeyboardConfig.AUTO_OPEN_OTHERS.get()
                && mc.screen != null && !KeyboardScreen.isOpen(mc) && !(mc.screen instanceof KeyboardScreen)
                && !suppressAutoOpen) {
            EditBox focused = findFocusedEditBoxRecursive(mc.screen);
            if (focused != null && focused != lastFocusedEditBox) {
                openKeyboardOnScreen(mc, mc.screen);
            }
            lastFocusedEditBox = focused;
        } else if (mc.screen == null) {
            lastFocusedEditBox = null;
            suppressAutoOpen = false;
        }

        // --- Delayed open (from K key → ChatScreen) ---
        if (pendingKeyboardOpen) {
            pendingKeyboardOpen = false;
            if (mc.screen != null && !(mc.screen instanceof KeyboardScreen) && !KeyboardScreen.isOpen(mc)) {
                openKeyboardOnScreen(mc, mc.screen);
            }
        }

        // --- Manual K key toggle ---
        if (toggleKeyMapping.consumeClick()) {
            handleTogglePress(mc);
        }
    }

    private void handleTogglePress(Minecraft mc) {
        if (KeyboardScreen.isOpen(mc)) {
            mc.setScreen(((KeyboardScreen) mc.screen).getBackgroundScreen());
            suppressAutoOpen = true;  // Don't re-open immediately
            return;
        }
        if (mc.screen == null) {
            pendingKeyboardOpen = true;
            mc.setScreen(new ChatScreen(""));
            return;
        }
        if (mc.screen instanceof KeyboardScreen) return;
        // Don't open if an EditBox already has focus (would cause double-open with auto-open)
        if (findFocusedEditBoxRecursive(mc.screen) != null) return;

        openKeyboardOnScreen(mc, mc.screen);
    }

    private void openKeyboardOnScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        EditBox editBox = findEditBoxRecursive(screen);
        InputTarget target;
        if (editBox != null) {
            target = KeyboardInputHandler.createInputTarget(editBox);
        } else {
            target = new FallbackInputTarget();
        }
        KeyboardScreen.open(screen, target);
    }

    /** Find a *focused* EditBox recursively. */
    private static EditBox findFocusedEditBoxRecursive(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null) return null;
        return findFocusedEditBoxInList(screen.children());
    }

    private static EditBox findFocusedEditBoxInList(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children) {
        for (var child : children) {
            if (child instanceof EditBox editBox && editBox.isFocused()) return editBox;
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
                EditBox found = findFocusedEditBoxInList(container.children());
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Find any EditBox recursively (not necessarily focused). */
    private static EditBox findEditBoxRecursive(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof ChatScreen chatScreen) {
            try {
                var field = ChatScreen.class.getDeclaredField("input");
                field.setAccessible(true);
                return (EditBox) field.get(chatScreen);
            } catch (Exception ignored) {}
        }
        if (screen == null) return null;
        return findEditBoxInList(screen.children());
    }

    private static EditBox findEditBoxInList(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children) {
        for (var child : children) {
            if (child instanceof EditBox editBox) return editBox;
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
                EditBox found = findEditBoxInList(container.children());
                if (found != null) return found;
            }
        }
        return null;
    }

    private static class FallbackInputTarget implements InputTarget {
        @Override public void acceptChar(char ch) {}
        @Override public void acceptSpecial(SpecialKey key) {}
    }
}
