package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    /**
     * Find ALL EditBoxes on the given screen, recursively.
     */
    public static List<EditBox> findAllEditBoxes(Screen screen) {
        List<EditBox> result = new ArrayList<>();
        if (screen instanceof ChatScreen chatScreen) {
            try {
                var field = ChatScreen.class.getDeclaredField("input");
                field.setAccessible(true);
                var eb = (EditBox) field.get(chatScreen);
                if (eb != null) result.add(eb);
            } catch (Exception ignored) {}
        }
        if (screen != null) {
            collectBoxes(screen.children(), result);
        }
        return result;
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
     * Find the currently FOCUSED EditBox from all available ones.
     */
    public static EditBox findFocusedEditBox(Screen screen) {
        for (var eb : findAllEditBoxes(screen)) {
            if (eb.isFocused()) return eb;
        }
        // Fallback: return first one if none is focused
        var all = findAllEditBoxes(screen);
        return all.isEmpty() ? null : all.get(0);
    }

    public static class SimpleInputTarget implements InputTarget {
        private final List<EditBox> allEditBoxes;
        private final Screen screen;

        public SimpleInputTarget(Screen screen) {
            this.screen = screen;
            this.allEditBoxes = findAllEditBoxes(screen);
        }

        private EditBox getTarget() {
            // Always try to find the currently focused one first
            for (var eb : allEditBoxes) {
                if (eb.isFocused()) return eb;
            }
            return allEditBoxes.isEmpty() ? null : allEditBoxes.get(0);
        }

        @Override
        public void acceptChar(char ch) {
            var target = getTarget();
            if (target != null) target.charTyped(ch, 0);
        }

        @Override
        public void acceptSpecial(SpecialKey key) {
            var target = getTarget();
            if (target == null) return;
            switch (key) {
                case BACKSPACE -> {
                    String val = target.getValue();
                    if (!val.isEmpty()) target.setValue(val.substring(0, val.length() - 1));
                }
                case ENTER -> {
                    target.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                }
            }
        }
    }
}
