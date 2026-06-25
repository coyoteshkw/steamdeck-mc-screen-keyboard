package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class KeyboardInputHandler {

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

        // Try to find JEI search box via its internal IngredientFilter
        EditBox jeiBox = findJeiSearchBox();
        if (jeiBox != null) result.add(jeiBox);

        // Try EMI search box
        EditBox emiBox = findEmiSearchBox();
        if (emiBox != null) result.add(emiBox);

        if (screen != null) {
            collectBoxes(screen.children(), result);
        }
        return result;
    }

    /**
     * Access JEI's internal IngredientFilter to get its search EditBox.
     * JEI stores it as IngredientFilter -> searchField (GuiTextFieldFilter extends EditBox).
     */
    private static EditBox findJeiSearchBox() {
        try {
            // JEI's IngredientFilter is usually accessed via the runtime
            // Try: mezz.jei.common.InternalAccess -> runtime -> IngredientFilter
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            Field runtimeField = internalClass.getDeclaredField("runtime");
            runtimeField.setAccessible(true);
            Object runtime = runtimeField.get(null);
            if (runtime == null) return null;

            // runtime.getIngredientFilter()
            var getFilterMethod = runtime.getClass().getMethod("getIngredientFilter");
            Object filter = getFilterMethod.invoke(runtime);
            if (filter == null) return null;

            // IngredientFilter has a field searchField of type GuiTextFieldFilter
            Field searchField = findField(filter.getClass(), "searchField");
            if (searchField != null) {
                searchField.setAccessible(true);
                Object value = searchField.get(filter);
                if (value instanceof EditBox eb) return eb;
            }
        } catch (Exception e) {
            // JEI not installed or API changed — that's fine
        }
        return null;
    }

    private static EditBox findEmiSearchBox() {
        try {
            // EMI: dev.emi.emi.screen.EmiScreenManager -> search field
            Class<?> screenMgrClass = Class.forName("dev.emi.emi.screen.EmiScreenManager");
            Field searchField = screenMgrClass.getDeclaredField("search");
            searchField.setAccessible(true);
            Object search = searchField.get(null);
            if (search instanceof EditBox eb) return eb;
        } catch (Exception e) {
            // EMI not installed
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
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

        public SimpleInputTarget(Screen screen) {
            this.screen = screen;
        }

        private EditBox getTarget() {
            var all = findAllEditBoxes(screen);
            for (var eb : all) { if (eb.isFocused()) return eb; }
            // Prefer JEI/EMI search boxes over vanilla ones
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
