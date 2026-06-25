package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    public static InputTarget createInputTarget(EditBox editBox) {
        return new InputTarget() {
            @Override
            public void acceptChar(char ch) {
                editBox.charTyped(ch, 0);
            }
            @Override
            public void acceptSpecial(SpecialKey key) {
                switch (key) {
                    case BACKSPACE -> {
                        String val = editBox.getValue();
                        if (!val.isEmpty()) editBox.setValue(val.substring(0, val.length() - 1));
                    }
                    case ENTER -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.screen instanceof KeyboardScreen ks) {
                            ks.getBackgroundScreen().keyPressed(
                                com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                        } else if (mc.screen != null) {
                            mc.screen.keyPressed(
                                com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                        }
                    }
                }
            }
        };
    }

    public static void openKeyboardForScreen(Screen screen) {
        EditBox editBox = findAnyEditBox(screen);
        if (editBox != null) {
            KeyboardScreen.open(screen, createInputTarget(editBox));
        }
    }

    private static EditBox findAnyEditBox(Screen screen) {
        if (screen instanceof ChatScreen chatScreen) {
            try {
                var field = ChatScreen.class.getDeclaredField("input");
                field.setAccessible(true);
                return (EditBox) field.get(chatScreen);
            } catch (Exception ignored) {}
        }
        if (screen == null) return null;
        var boxes = new ArrayList<EditBox>();
        collectBoxes(screen.children(), boxes);
        return boxes.isEmpty() ? null : boxes.get(0);
    }

    private static void collectBoxes(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, List<EditBox> out) {
        for (var child : children) {
            if (child instanceof EditBox eb) out.add(eb);
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler ce) {
                collectBoxes(ce.children(), out);
            }
        }
    }
}
