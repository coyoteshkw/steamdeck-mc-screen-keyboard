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

import java.util.ArrayList;
import java.util.List;

@Mod(value = SteamDeckKeyboard.MODID, dist = Dist.CLIENT)
public class SteamDeckKeyboardClient {
    private static KeyMapping toggleKeyMapping;
    private static boolean pendingKeyboardOpen = false;
    static boolean suppressAutoOpen = false;
    // Track the last screen where we auto-opened to avoid repeated opens
    private static String lastAutoOpenedScreen = null;

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

        // --- Auto-open: when entering a screen that has an EditBox ---
        if (KeyboardConfig.AUTO_OPEN_OTHERS.get()
                && mc.screen != null && !KeyboardScreen.isOpen(mc) && !(mc.screen instanceof KeyboardScreen)
                && !suppressAutoOpen) {
            String screenId = mc.screen.getClass().getName();
            if (!screenId.equals(lastAutoOpenedScreen)) {
                // New screen entered — check if it has any EditBox
                List<EditBox> boxes = findAllEditBoxes(mc.screen);
                if (!boxes.isEmpty()) {
                    openKeyboardOnScreen(mc, mc.screen);
                }
                lastAutoOpenedScreen = screenId;
            }
        } else if (mc.screen == null) {
            lastAutoOpenedScreen = null;
            suppressAutoOpen = false;
        }

        // --- Delayed open (from K key -> ChatScreen) ---
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
        EditBox editBox = findAnyEditBox(screen);
        InputTarget target;
        if (editBox != null) {
            target = KeyboardInputHandler.createInputTarget(editBox);
        } else {
            target = new FallbackInputTarget();
        }
        KeyboardScreen.open(screen, target);
    }

    private static List<EditBox> findAllEditBoxes(net.minecraft.client.gui.screens.Screen screen) {
        List<EditBox> result = new ArrayList<>();
        if (screen != null) collectEditBoxes(screen.children(), result);
        return result;
    }

    private static void collectEditBoxes(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, List<EditBox> out) {
        for (var child : children) {
            if (child instanceof EditBox editBox) out.add(editBox);
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
                collectEditBoxes(container.children(), out);
            }
        }
    }

    private static EditBox findAnyEditBox(net.minecraft.client.gui.screens.Screen screen) {
        var boxes = findAllEditBoxes(screen);
        return boxes.isEmpty() ? null : boxes.get(0);
    }

    private static class FallbackInputTarget implements InputTarget {
        @Override public void acceptChar(char ch) {}
        @Override public void acceptSpecial(SpecialKey key) {}
    }
}
