package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    private static boolean jeiLookupAttempted = false;
    private static Field jeiSearchField = null;
    private static Object jeiRuntime = null;

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

        // Lazy-load JEI search box (only try once)
        if (!jeiLookupAttempted) {
            jeiLookupAttempted = true;
            try {
                Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
                Field rf = internalClass.getDeclaredField("runtime");
                rf.setAccessible(true);
                jeiRuntime = rf.get(null);
                if (jeiRuntime != null) {
                    var m = jeiRuntime.getClass().getMethod("getIngredientFilter");
                    Object filter = m.invoke(jeiRuntime);
                    if (filter != null) {
                        jeiSearchField = findField(filter.getClass(), "searchField");
                        if (jeiSearchField != null) jeiSearchField.setAccessible(true);
                    }
                }
            } catch (Exception e) {
                SteamDeckKeyboard.LOGGER.debug("JEI reflection setup failed: {}", e.toString());
            }
        }

        // Try to get current JEI search box instance
        if (jeiRuntime != null && jeiSearchField != null) {
            try {
                var m = jeiRuntime.getClass().getMethod("getIngredientFilter");
                Object filter = m.invoke(jeiRuntime);
                if (filter != null) {
                    Object value = jeiSearchField.get(filter);
                    if (value instanceof EditBox eb) result.add(eb);
                }
            } catch (Exception ignored) {}
        }

        // Try EMI search box
        try {
            Class<?> emiClass = Class.forName("dev.emi.emi.screen.EmiScreenManager");
            Field sf = emiClass.getDeclaredField("search");
            sf.setAccessible(true);
            Object search = sf.get(null);
            if (search instanceof EditBox eb) result.add(eb);
        } catch (Exception ignored) {}

        if (screen != null) {
            collectBoxes(screen.children(), result);
        }
        return result;
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(name); }
            catch (NoSuchFieldException e) { current = current.getSuperclass(); }
        }
        return null;
    }

    private static void collectBoxes(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, List<EditBox> out) {
        for (var child : children) {
            if (child instanceof EditBox eb) out.add(eb);
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler ce) {
                collectBoxes(ce.children(), out);
            }
        }
    }

    public static class SimpleInputTarget implements InputTarget {
        private final Screen screen;

        public SimpleInputTarget(Screen screen) {
            this.screen = screen;
        }

        private EditBox getTarget() {
            var all = findAllEditBoxes(screen);
            for (var eb : all) { if (eb.isFocused()) return eb; }
            for (var eb : all) {
                String name = eb.getClass().getSimpleName();
                if (name.contains("Filter") || name.contains("Search") || name.contains("search")) return eb;
            }
            return all.isEmpty() ? null : all.get(0);
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
                case ENTER -> target.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
            }
        }
    }
}
