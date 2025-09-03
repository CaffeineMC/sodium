package net.caffeinemc.mods.sodium.client.render.model;

import net.minecraft.util.TriState;

public enum AmbientOcclusionMode {
    ENABLED,
    DEFAULT,
    DISABLED;

    private static final TriState[] TRISTATES = new TriState[] {
        TriState.TRUE,    // ENABLED
        TriState.DEFAULT, // DEFAULT
        TriState.FALSE    // DISABLED
    };

    public TriState toTriState() {
        return TRISTATES[this.ordinal()];
    }
}
