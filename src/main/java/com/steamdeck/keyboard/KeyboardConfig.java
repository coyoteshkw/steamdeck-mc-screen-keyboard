package com.steamdeck.keyboard;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

public class KeyboardConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TOGGLE_KEY_CODE = BUILDER
            .comment("Default key code for toggle keyboard (GLFW_KEY_K = 75)")
            .defineInRange("toggleKeyCode", GLFW.GLFW_KEY_K, 0, 348);

    public static final ModConfigSpec.BooleanValue AUTO_OPEN_CHAT = BUILDER
            .comment("Auto-open keyboard when chat field gains focus")
            .define("autoOpenChat", true);

    public static final ModConfigSpec.BooleanValue AUTO_OPEN_SEARCH = BUILDER
            .comment("Auto-open keyboard when JEI/EMI search field gains focus")
            .define("autoOpenSearch", true);

    public static final ModConfigSpec.BooleanValue AUTO_OPEN_OTHERS = BUILDER
            .comment("Auto-open keyboard when other EditBox gains focus")
            .define("autoOpenOthers", false);

    public static final ModConfigSpec.IntValue KEYBOARD_X = BUILDER
            .comment("Saved keyboard X position (-1 = center)")
            .defineInRange("keyboardX", -1, -1, 10000);

    public static final ModConfigSpec.IntValue KEYBOARD_Y = BUILDER
            .comment("Saved keyboard Y position (-1 = auto near bottom)")
            .defineInRange("keyboardY", -1, -1, 10000);

    public static final ModConfigSpec.IntValue KEYBOARD_WIDTH = BUILDER
            .comment("Saved keyboard width (-1 = 90% of screen)")
            .defineInRange("keyboardWidth", -1, -1, 10000);

    public static final ModConfigSpec.IntValue KEYBOARD_HEIGHT = BUILDER
            .comment("Saved keyboard height (-1 = 30% of screen)")
            .defineInRange("keyboardHeight", -1, -1, 10000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
