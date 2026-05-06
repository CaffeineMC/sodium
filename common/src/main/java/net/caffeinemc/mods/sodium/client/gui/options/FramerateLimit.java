package net.caffeinemc.mods.sodium.client.gui.options;

import net.minecraft.resources.Identifier;

public final class FramerateLimit {
    public static final Identifier OPTION_ID = Identifier.parse("sodium:general.framerate_limit");

    public static final int MIN = 10;
    public static final int MAX = 1_000_000;
    public static final int SODIUM_DEFAULT = 60;
    public static final int VANILLA_DEFAULT = 120;

    private FramerateLimit() {
    }
}
