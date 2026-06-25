package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    public static EditBox findAnyEditBox(Screen screen) {
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

    /**
     * Simple input target that uses EditBox directly.
     */
    public static class SimpleInputTarget implements InputTarget {
        private final EditBox editBox;

        public SimpleInputTarget(EditBox editBox) {
            this.editBox = editBox;
        }

        @Override
        public void acceptChar(char ch) {
            if (editBox != null) editBox.charTyped(ch, 0);
        }

        @Override
        public void acceptSpecial(SpecialKey key) {
            if (editBox == null) return;
            switch (key) {
                case BACKSPACE -> {
                    String val = editBox.getValue();
                    if (!val.isEmpty()) editBox.setValue(val.substring(0, val.length() - 1));
                }
                case ENTER -> {
                    editBox.keyPressed(
                        com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                }
            }
        }
    }
}
