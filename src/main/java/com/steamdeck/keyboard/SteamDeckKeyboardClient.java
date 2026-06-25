package com.steamdeck.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
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
        NeoForge.EVENT_BUS.addListener(this::onScreenInit);
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

    /**
     * Add a "Keyboard" button to inventory-style screens so the player
     * can manually open the keyboard for searching.
     */
    private void onScreenInit(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
            // Add a small keyboard button in the top-right area
            int btnX = screen.width - 70;
            int btnY = 5;
            event.addListener(
                Button.builder(Component.literal("\u2328"), btn -> {
                    KeyboardInputHandler.openKeyboardForScreen(screen);
                })
                .pos(btnX, btnY)
                .size(20, 20)
                .build()
            );
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || toggleKeyMapping == null) return;

        if (pendingKeyboardOpen) {
            pendingKeyboardOpen = false;
            if (mc.screen != null && !(mc.screen instanceof KeyboardScreen) && !KeyboardScreen.isOpen(mc)) {
                KeyboardInputHandler.openKeyboardForScreen(mc.screen);
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

        KeyboardInputHandler.openKeyboardForScreen(mc.screen);
    }
}
