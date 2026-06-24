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
import net.neoforged.neoforge.client.settings.KeyConflictContext;
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
            KeyConflictContext.GUI,
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
            if (KeyboardScreen.isOpen(mc)) {
                // Close keyboard
                mc.setScreen(((KeyboardScreen) mc.screen).getBackgroundScreen());
            } else if (mc.screen != null) {
                // Open keyboard if there's an EditBox to target
                EditBox editBox = KeyboardInputHandler.findFocusedEditBox(mc.screen);
                if (editBox != null) {
                    InputTarget target = KeyboardInputHandler.createInputTarget(editBox);
                    KeyboardScreen.open(mc.screen, target);
                }
            }
        }
        wasToggleKeyDown = down;
    }
}
