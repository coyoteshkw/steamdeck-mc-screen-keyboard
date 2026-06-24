package com.steamdeck.keyboard.mixin;

import com.steamdeck.keyboard.InputTarget;
import com.steamdeck.keyboard.KeyboardConfig;
import com.steamdeck.keyboard.KeyboardScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements InputTarget {
    @Shadow protected EditBox input;
    @Unique private float keyboardShiftAmount = 0f;

    protected ChatScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (KeyboardConfig.AUTO_OPEN_CHAT.get() && !KeyboardScreen.isOpen(minecraft)) {
            KeyboardScreen.open(this, this);
            keyboardShiftAmount = 0.25f;
        }
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 3)
    private int modifyEditBoxY(int originalY) {
        if (keyboardShiftAmount > 0) {
            return height - (int)(height * keyboardShiftAmount) - 12;
        }
        return originalY;
    }

    @Override
    public void acceptChar(char ch) {
        if (input != null) {
            input.charTyped(new CharacterEvent(ch));
        }
    }

    @Override
    public void acceptSpecial(SpecialKey key) {
        switch (key) {
            case BACKSPACE -> {
                if (input != null) {
                    String value = input.getValue();
                    if (!value.isEmpty()) {
                        input.setValue(value.substring(0, value.length() - 1));
                    }
                }
            }
            case ENTER -> {
                if (minecraft != null) {
                    minecraft.setScreen(null);
                }
            }
        }
    }
}
