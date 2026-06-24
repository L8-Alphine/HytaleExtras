package org.hyzionstudios.hyextras.triggerextras.advanced;

import java.util.Map;

public enum TargetSelector {
    PLAYER,
    PLAYERS,
    ENTITIES;

    public static final Map<TargetSelector, String> ALIASES = Map.of(
            PLAYER, "player",
            PLAYERS, "players",
            ENTITIES, "entities"
    );
}
