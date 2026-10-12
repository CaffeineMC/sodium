package net.caffeinemc.mods.sodium.client.gui.options;

public enum FullscreenMode {
    OFF,
    EXCLUSIVE,
    BORDERLESS;

    public static FullscreenMode of(boolean fullscreen, boolean exclusive) {
        if (!fullscreen) {
            return OFF;
        }
        return exclusive ? EXCLUSIVE : BORDERLESS;
    }
}
