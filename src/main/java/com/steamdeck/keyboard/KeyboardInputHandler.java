package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    public static EditBox findFocusedEditBox(Screen screen) {
        if (screen == null) return null;
        return findFocusedRecursive(screen.children());
    }

    private static EditBox findFocusedRecursive(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children) {
        for (var child : children) {
            if (child instanceof EditBox editBox && editBox.isFocused()) return editBox;
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
                EditBox found = findFocusedRecursive(container.children());
                if (found != null) return found;
            }
        }
        return null;
    }

    private static EditBox findAnyEditBox(Screen screen) {
        if (screen instanceof ChatScreen chatScreen) {
            try {
                var field = ChatScreen.class.getDeclaredField("input");
                field.setAccessible(true);
                return (EditBox) field.get(chatScreen);
            } catch (Exception ignored) {}
        }
        var boxes = new ArrayList<EditBox>();
        if (screen != null) collectEditBoxes(screen.children(), boxes);
        return boxes.isEmpty() ? null : boxes.get(0);
    }

    private static void collectEditBoxes(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, List<EditBox> out) {
        for (var child : children) {
            if (child instanceof EditBox editBox) out.add(editBox);
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) {
                collectEditBoxes(container.children(), out);
            }
        }
    }

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
                        if (!val.isEmpty()) {
                            editBox.setValue(val.substring(0, val.length() - 1));
                        }
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
        InputTarget target = editBox != null ? createInputTarget(editBox) : new InputTarget() {
            @Override public void acceptChar(char ch) {}
            @Override public void acceptSpecial(SpecialKey key) {}
        };
        KeyboardScreen.open(screen, target);
    }
}
