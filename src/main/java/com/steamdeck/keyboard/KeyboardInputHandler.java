package com.steamdeck.keyboard;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    private static boolean jeiInitDone = false;
    private static java.lang.reflect.Method jeiGetFilterMethod = null;
    private static Field jeiSearchField = null;

    public static List<EditBox> findAllEditBoxes(Screen screen) {
        List<EditBox> result = new ArrayList<>();

        if (screen instanceof ChatScreen chatScreen) {
            try {
                var f = ChatScreen.class.getDeclaredField("input");
                f.setAccessible(true);
                var eb = (EditBox) f.get(chatScreen);
                if (eb != null) result.add(eb);
            } catch (Exception ignored) {}
        }

        // JEI search box via reflection (one-time setup)
        if (!jeiInitDone) {
            jeiInitDone = true;
            try {
                Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
                Object runtime = internalClass.getMethod("getJeiRuntime").invoke(null);
                if (runtime != null) {
                    jeiGetFilterMethod = runtime.getClass().getMethod("getIngredientFilter");
                    Object filter = jeiGetFilterMethod.invoke(runtime);
                    if (filter != null) {
                        jeiSearchField = findField(filter.getClass(), "searchField");
                        if (jeiSearchField != null) jeiSearchField.setAccessible(true);
                    }
                }
            } catch (Exception e) {
                SteamDeckKeyboard.LOGGER.debug("JEI init: {}", e.toString());
            }
        }

        if (jeiGetFilterMethod != null && jeiSearchField != null) {
            try {
                Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
                Object runtime = internalClass.getMethod("getJeiRuntime").invoke(null);
                if (runtime != null) {
                    Object filter = jeiGetFilterMethod.invoke(runtime);
                    if (filter != null) {
                        Object val = jeiSearchField.get(filter);
                        if (val instanceof EditBox eb) result.add(eb);
                    }
                }
            } catch (Exception ignored) {}
        }

        // EMI search box
        try {
            Class<?> c = Class.forName("dev.emi.emi.screen.EmiScreenManager");
            Field f = c.getDeclaredField("search");
            f.setAccessible(true);
            Object s = f.get(null);
            if (s instanceof EditBox eb) result.add(eb);
        } catch (Exception ignored) {}

        if (screen != null) {
            collectBoxes(screen.children(), result);
        }
        return result;
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> cur = clazz;
        while (cur != null) {
            try { return cur.getDeclaredField(name); }
            catch (NoSuchFieldException e) { cur = cur.getSuperclass(); }
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
        public SimpleInputTarget(Screen screen) { this.screen = screen; }

        private EditBox getTarget() {
            var all = findAllEditBoxes(screen);
            for (var eb : all) { if (eb.isFocused()) return eb; }
            for (var eb : all) {
                String n = eb.getClass().getSimpleName();
                if (n.contains("Filter") || n.contains("Search") || n.contains("search")) return eb;
            }
            return all.isEmpty() ? null : all.get(0);
        }

        @Override public void acceptChar(char ch) {
            var t = getTarget();
            if (t != null) t.charTyped(ch, 0);
        }

        @Override public void acceptSpecial(SpecialKey key) {
            var t = getTarget();
            if (t == null) return;
            switch (key) {
                case BACKSPACE -> { String v = t.getValue(); if (!v.isEmpty()) t.setValue(v.substring(0, v.length() - 1)); }
                case ENTER -> t.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
            }
        }
    }
}
