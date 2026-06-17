package org.hyzionstudios.hytaleextras.action;

import java.util.Map;

public enum ToggleMode {
    ENABLE,
    DISABLE,
    TOGGLE;

    public static final Map<ToggleMode, String> ALIASES = Map.of(
            ENABLE, "enable",
            DISABLE, "disable",
            TOGGLE, "toggle"
    );
}
