package org.hyzionstudios.hyextras.action;

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
