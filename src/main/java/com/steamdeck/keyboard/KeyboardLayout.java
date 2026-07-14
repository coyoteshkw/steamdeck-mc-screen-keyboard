package com.steamdeck.keyboard;

import java.util.List;

public final class KeyboardLayout {
    public enum KeyType { CHAR, BACKSPACE, ENTER, SHIFT, SPACE, CLOSE, TAB, ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT }

    public record KeyDef(String name, char normal, char shifted, KeyType keyType, float width) {}

    public static final List<List<KeyDef>> ROWS = List.of(
        // Number row
        List.of(
            new KeyDef("1", '1', '!', KeyType.CHAR, 1f),
            new KeyDef("2", '2', '@', KeyType.CHAR, 1f),
            new KeyDef("3", '3', '#', KeyType.CHAR, 1f),
            new KeyDef("4", '4', '$', KeyType.CHAR, 1f),
            new KeyDef("5", '5', '%', KeyType.CHAR, 1f),
            new KeyDef("6", '6', '^', KeyType.CHAR, 1f),
            new KeyDef("7", '7', '&', KeyType.CHAR, 1f),
            new KeyDef("8", '8', '*', KeyType.CHAR, 1f),
            new KeyDef("9", '9', '(', KeyType.CHAR, 1f),
            new KeyDef("0", '0', ')', KeyType.CHAR, 1f),
            new KeyDef("minus", '-', '_', KeyType.CHAR, 1f),
            new KeyDef("equals", '=', '+', KeyType.CHAR, 1f)
        ),
        // Top letter row
        List.of(
            new KeyDef("q", 'q', 'Q', KeyType.CHAR, 1f),
            new KeyDef("w", 'w', 'W', KeyType.CHAR, 1f),
            new KeyDef("e", 'e', 'E', KeyType.CHAR, 1f),
            new KeyDef("r", 'r', 'R', KeyType.CHAR, 1f),
            new KeyDef("t", 't', 'T', KeyType.CHAR, 1f),
            new KeyDef("y", 'y', 'Y', KeyType.CHAR, 1f),
            new KeyDef("u", 'u', 'U', KeyType.CHAR, 1f),
            new KeyDef("i", 'i', 'I', KeyType.CHAR, 1f),
            new KeyDef("o", 'o', '{', KeyType.CHAR, 1f),
            new KeyDef("p", 'p', '}', KeyType.CHAR, 1f),
            new KeyDef("backspace", '\b', '\b', KeyType.BACKSPACE, 2f)
        ),
        // Home row
        List.of(
            new KeyDef("tab", '\t', '\t', KeyType.TAB, 1f),
            new KeyDef("a", 'a', 'A', KeyType.CHAR, 1f),
            new KeyDef("s", 's', 'S', KeyType.CHAR, 1f),
            new KeyDef("d", 'd', 'D', KeyType.CHAR, 1f),
            new KeyDef("f", 'f', 'F', KeyType.CHAR, 1f),
            new KeyDef("g", 'g', 'G', KeyType.CHAR, 1f),
            new KeyDef("h", 'h', 'H', KeyType.CHAR, 1f),
            new KeyDef("j", 'j', 'J', KeyType.CHAR, 1f),
            new KeyDef("k", 'k', 'K', KeyType.CHAR, 1f),
            new KeyDef("l", 'l', 'L', KeyType.CHAR, 1f),
            new KeyDef("slash", '/', '?', KeyType.CHAR, 1f),
            new KeyDef("enter", '\n', '\n', KeyType.ENTER, 1f)
        ),
        // Bottom letter row
        List.of(
            new KeyDef("shift", '\0', '\0', KeyType.SHIFT, 2f),
            new KeyDef("z", 'z', 'Z', KeyType.CHAR, 1f),
            new KeyDef("x", 'x', 'X', KeyType.CHAR, 1f),
            new KeyDef("c", 'c', 'C', KeyType.CHAR, 1f),
            new KeyDef("v", 'v', 'V', KeyType.CHAR, 1f),
            new KeyDef("b", 'b', 'B', KeyType.CHAR, 1f),
            new KeyDef("n", 'n', 'N', KeyType.CHAR, 1f),
            new KeyDef("m", 'm', 'M', KeyType.CHAR, 1f),
            new KeyDef("comma", ',', ';', KeyType.CHAR, 1f),
            new KeyDef("period", '.', '\'', KeyType.CHAR, 1f)
        ),
        // Space + arrows row
        List.of(
            new KeyDef("space", ' ', ' ', KeyType.SPACE, 6f),
            new KeyDef("semicolon", ';', ':', KeyType.CHAR, 1f),
            new KeyDef("left", '\0', '\0', KeyType.ARROW_LEFT, 1f),
            new KeyDef("down", '\0', '\0', KeyType.ARROW_DOWN, 1f),
            new KeyDef("up", '\0', '\0', KeyType.ARROW_UP, 1f),
            new KeyDef("right", '\0', '\0', KeyType.ARROW_RIGHT, 1f),
            new KeyDef("close", '\0', '\0', KeyType.CLOSE, 1f)
        )
    );

    public static final float LAYOUT_WIDTH = 12f;

    private KeyboardLayout() {}
}
