package com.github.thecrazy.noborders;

public enum WindowMode {
    WINDOWED, FULLSCREEN, BORDERLESS;

    public WindowMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "noborders.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
