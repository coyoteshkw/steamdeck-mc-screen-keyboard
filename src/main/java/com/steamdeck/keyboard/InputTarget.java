package com.steamdeck.keyboard;

public interface InputTarget {
    enum SpecialKey { BACKSPACE, ENTER, TAB, ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT }

    void acceptChar(char ch);
    void acceptSpecial(SpecialKey key);

    default boolean supportsCharInput() { return true; }
}
