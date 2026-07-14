package com.steamdeck.keyboard;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

    private static boolean jeiInitDone = false;
    private static java.lang.reflect.Method jeiGetOverlayMethod = null;
    private static Field jeiSearchField = null;
    private static Class<?> jeiInternalClass = null;

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

        // JEI search box via reflection: Internal -> getJeiRuntime() -> getIngredientListOverlay() -> searchField
        initJei();

        if (jeiInternalClass != null && jeiGetOverlayMethod != null && jeiSearchField != null) {
            try {
                Object runtime = jeiInternalClass.getMethod("getJeiRuntime").invoke(null);
                if (runtime != null) {
                    Object overlay = jeiGetOverlayMethod.invoke(runtime);
                    if (overlay != null) {
                        Object val = jeiSearchField.get(overlay);
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
            for (var r : screen.renderables) {
                if (r instanceof EditBox eb && !result.contains(eb)) result.add(eb);
                if (r instanceof net.minecraft.client.gui.components.events.ContainerEventHandler ce) {
                    collectBoxes(ce.children(), result);
                }
            }
        }
        if (result.isEmpty()) {
            SteamDeckKeyboard.LOGGER.warn("findAllEditBoxes: no EditBoxes found for screen {}", screen);
        }
        return result;
    }

    private static void initJei() {
        if (jeiInitDone) return;
        jeiInitDone = true;

        try {
            jeiInternalClass = Class.forName("mezz.jei.common.Internal");
            Object runtime = jeiInternalClass.getMethod("getJeiRuntime").invoke(null);
            if (runtime != null) {
                jeiGetOverlayMethod = runtime.getClass().getMethod("getIngredientListOverlay");
                Object overlay = jeiGetOverlayMethod.invoke(runtime);
                if (overlay != null) {
                    // actual class is IngredientListOverlay which has the searchField
                    jeiSearchField = findField(overlay.getClass(), "searchField");
                    if (jeiSearchField != null) {
                        jeiSearchField.setAccessible(true);
                        SteamDeckKeyboard.LOGGER.info("JEI search field found on {}", overlay.getClass().getName());
                    }
                }
            }
        } catch (Exception e) {
            SteamDeckKeyboard.LOGGER.warn("JEI init failed: {}", e.toString());
        }
    }

    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            Class<?> cur = clazz;
            while (cur != null) {
                try { return cur.getDeclaredField(name); }
                catch (NoSuchFieldException e) { cur = cur.getSuperclass(); }
            }
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
        private EditBox lastTarget;

        public SimpleInputTarget(Screen screen) { this.screen = screen; }

        private EditBox getTarget() {
            var all = findAllEditBoxes(screen);
            for (var eb : all) { if (eb.isFocused()) { lastTarget = eb; return eb; } }
            if (lastTarget != null && all.contains(lastTarget)) return lastTarget;
            for (var eb : all) {
                String n = eb.getClass().getSimpleName();
                if (n.contains("Filter") || n.contains("Search") || n.contains("search")) return eb;
            }
            return all.isEmpty() ? null : all.get(0);
        }

        @Override public void acceptChar(char ch) {
            var t = getTarget();
            if (t == null) {
                // No EditBox found by our search — try screen-level charTyped
                screen.charTyped(ch, 0);
                return;
            }
            t.setFocused(true);
            t.setEditable(true);
            if (!t.charTyped(ch, 0)) {
                insertCharDirect(t, ch);
            }
        }

        /** Bypasses EditBox focus checks by directly inserting at cursor position. */
        private void insertCharDirect(EditBox eb, char ch) {
            String old = eb.getValue();
            int cursor = eb.getCursorPosition();
            if (cursor < 0 || cursor > old.length()) cursor = old.length();
            eb.setValue(old.substring(0, cursor) + ch + old.substring(cursor));
            eb.moveCursorTo(cursor + 1, false);
        }

        @Override public void acceptSpecial(SpecialKey key) {
            var t = getTarget();
            if (t == null) return;
            switch (key) {
                case BACKSPACE -> {
                    String v = t.getValue();
                    if (v.isEmpty()) break;
                    t.setFocused(true);
                    t.setEditable(true);
                    int cursor = t.getCursorPosition();
                    if (cursor <= 0 || cursor > v.length()) {
                        if (!t.charTyped('\b', 0)) {
                            t.setValue(v.substring(0, v.length() - 1));
                        }
                    } else {
                        t.setValue(v.substring(0, cursor - 1) + v.substring(cursor));
                        t.moveCursorTo(cursor - 1, false);
                    }
                }
                case TAB -> screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_TAB, 0, 0);
                case ENTER -> screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                case ARROW_UP -> screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_UP, 0, 0);
                case ARROW_DOWN -> screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_DOWN, 0, 0);
                case ARROW_LEFT -> screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_LEFT, 0, 0);
                case ARROW_RIGHT -> screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT, 0, 0);
            }
        }
    }
}
