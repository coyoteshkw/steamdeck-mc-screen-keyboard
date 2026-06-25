package com.steamdeck.keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Handles keyboard input by directly manipulating the active EditBox on the current screen.
 * Walks the widget tree of the current screen to find the focused EditBox.
 */
public class KeyboardInputHandler {

    /**
     * Finds the currently focused EditBox on the given screen.
     * Returns null if no EditBox is focused.
     */
    public static EditBox findFocusedEditBox(Screen screen) {
        if (screen == null) return null;

        // ChatScreen has a known EditBox field
        if (screen instanceof ChatScreen chatScreen) {
            // Access the protected 'input' field via reflection
            try {
                var field = ChatScreen.class.getDeclaredField("input");
                field.setAccessible(true);
                return (EditBox) field.get(chatScreen);
            } catch (Exception e) {
                SteamDeckKeyboard.LOGGER.warn("Could not access ChatScreen.input field", e);
            }
        }

        // For other screens, try to find EditBox among children
        for (var child : screen.children()) {
            if (child instanceof EditBox editBox && editBox.isFocused()) {
                return editBox;
            }
        }

        return null;
    }

    /**
     * Creates an InputTarget that writes to the given EditBox.
     */
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
                // Let the screen handle Enter via its own keyPressed —
                // ChatScreen will send the message and close itself.
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen != null && !(mc.screen instanceof KeyboardScreen)) {
                    mc.screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0);
                }
            }
                }
            }
        };
    }
}
