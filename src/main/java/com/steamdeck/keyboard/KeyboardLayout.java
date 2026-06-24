package com.steamdeck.keyboard;

import java.util.List;

public final class KeyboardLayout {
    public enum KeyType { CHAR, BACKSPACE, ENTER, SHIFT, SPACE, CLOSE }

    public record KeyDef(String name, char normal, char shifted, KeyType keyType, float width) {}

    public static final List<List<KeyDef>> ROWS = List.of(
        List.of(
            new KeyDef("q", 'q', 'Q', KeyType.CHAR, 1f),
            new KeyDef("w", 'w', 'W', KeyType.CHAR, 1f),
            new KeyDef("e", 'e', 'E', KeyType.CHAR, 1f),
            new KeyDef("r", 'r', 'R', KeyType.CHAR, 1f),
            new KeyDef("t", 't', 'T', KeyType.CHAR, 1f),
            new KeyDef("y", 'y', 'Y', KeyType.CHAR, 1f),
            new KeyDef("u", 'u', 'U', KeyType.CHAR, 1f),
            new KeyDef("i", 'i', 'I', KeyType.CHAR, 1f),
            new KeyDef("o", 'o', 'O', KeyType.CHAR, 1f),
            new KeyDef("p", 'p', 'P', KeyType.CHAR, 1f),
            new KeyDef("backspace", '\b', '\b', KeyType.BACKSPACE, 2f)
        ),
        List.of(
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
            new KeyDef("enter", '\n', '\n', KeyType.ENTER, 2f)
        ),
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
        List.of(
            new KeyDef("space", ' ', ' ', KeyType.SPACE, 4f),
            new KeyDef("at", '@', '(', KeyType.CHAR, 1f),
            new KeyDef("hash", '#', ')', KeyType.CHAR, 1f),
            new KeyDef("dollar", '$', '-', KeyType.CHAR, 1f),
            new KeyDef("percent", '%', '+', KeyType.CHAR, 1f),
            new KeyDef("exclaim", '!', '=', KeyType.CHAR, 1f),
            new KeyDef("colon", ':', '[', KeyType.CHAR, 1f),
            new KeyDef("underscore", '_', '{', KeyType.CHAR, 1f),
            new KeyDef("ampersand", '&', '}', KeyType.CHAR, 1f),
            new KeyDef("close", '\0', '\0', KeyType.CLOSE, 1f)
        )
    );

    public static final float LAYOUT_WIDTH = 12f;

    private KeyboardLayout() {}
}
