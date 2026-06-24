package com.steamdeck.keyboard.mixin;

import com.steamdeck.keyboard.InputTarget;
import com.steamdeck.keyboard.KeyboardConfig;
import com.steamdeck.keyboard.KeyboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public class EditBoxMixin {

    @Inject(method = "setFocused", at = @At("HEAD"))
    private void onSetFocused(boolean focused, CallbackInfo ci) {
        if (!focused) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || KeyboardScreen.isOpen(mc)) return;

        boolean isChat = mc.screen instanceof ChatScreen;
        boolean isSearch = isSearchScreen(mc.screen);

        if (isChat && KeyboardConfig.AUTO_OPEN_CHAT.get()) {
            if (mc.screen instanceof ChatScreen cs) {
                KeyboardScreen.open(cs, (InputTarget) cs);
            }
        } else if (isSearch && KeyboardConfig.AUTO_OPEN_SEARCH.get()) {
            KeyboardScreen.open(mc.screen, createGenericInputTarget((EditBox)(Object)this));
        } else if (KeyboardConfig.AUTO_OPEN_OTHERS.get()) {
            KeyboardScreen.open(mc.screen, createGenericInputTarget((EditBox)(Object)this));
        }
    }

    private static boolean isSearchScreen(Screen screen) {
        String className = screen.getClass().getName();
        return className.contains("jei") || className.contains("emi");
    }

    private static InputTarget createGenericInputTarget(EditBox editBox) {
        return new InputTarget() {
            @Override
            public void acceptChar(char ch) {
                editBox.charTyped(new CharacterEvent(ch));
            }
            @Override
            public void acceptSpecial(SpecialKey key) {
                switch (key) {
                    case BACKSPACE -> {
                        String val = editBox.getValue();
                        if (!val.isEmpty()) editBox.setValue(val.substring(0, val.length() - 1));
                    }
                    case ENTER -> Minecraft.getInstance().setScreen(null);
                }
            }
        };
    }
}
