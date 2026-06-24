package com.steamdeck.keyboard;

public interface InputTarget {
    enum SpecialKey { BACKSPACE, ENTER }

    void acceptChar(char ch);
    void acceptSpecial(SpecialKey key);

    default boolean supportsCharInput() { return true; }
}
