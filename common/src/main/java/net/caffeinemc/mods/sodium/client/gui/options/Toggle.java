package net.caffeinemc.mods.sodium.client.gui.options;

/**
 * Two-state enum used to present boolean-backed options as enum controls in the options screen.
 */
public enum Toggle {
    OFF,
    ON;

    public static Toggle fromBoolean(boolean value) {
        return value ? ON : OFF;
    }

    public boolean toBoolean() {
        return this == ON;
    }
}
