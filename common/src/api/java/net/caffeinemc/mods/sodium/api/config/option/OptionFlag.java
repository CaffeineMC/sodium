package net.caffeinemc.mods.sodium.api.config.option;

/**
 * Flags that indicate specific actions required when an option is changed.
 */
public enum OptionFlag {
    /**
     * Indicates that the renderer needs to be reloaded. This means all meshes will be discarded and rebuilt. This flag should only be applied if necessary, since rebuilding all meshes is a disruptive operation.
     */
    REQUIRES_RENDERER_RELOAD,
    
    /**
     * Indicates that the renderer needs to be updated. This means that the culling state may have changed and occlusion culling will be recalculated. This causes no noticeable disruption to the user.
     */
    REQUIRES_RENDERER_UPDATE,
    
    /**
     * Indicates that assets need to be reloaded. This reloads resource packs, and then reloads the renderer, causing all meshes to be discarded and rebuilt.
     */
    REQUIRES_ASSET_RELOAD,
    
    /**
     * Indicates that the video mode needs to be updated. This causes a disruption as the video mode is changed.
     */
    REQUIRES_VIDEOMODE_RELOAD,
    
    /**
     * Indicates that the game needs to be restarted for the option change to take effect.
     */
    REQUIRES_GAME_RESTART
}
