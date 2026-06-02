package net.caffeinemc.mods.sodium.client.gpu;

import net.minecraft.util.Util;

public class GPULimits {
    public static int getSubTexelPrecisionBits() {
        // OpenGL only specifies "at least" 4 bits of sub-texel precision for texture fetches. Thankfully, nearly every
        // graphics card is Direct3D-compatible and capable of providing 8 bits of precision. The only exception to this
        // rule seems to be when using OpenGL on macOS, where it appears to arbitrarily limit the precision to 4 bits
        // *even if* the hardware is capable of better.
        // TODO: On Vulkan, this can be explicitly queried. However, it is a pain right now.
        if (Util.getPlatform() == Util.OS.OSX) {
            return 4;
        } else {
            return 8;
        }
    }
}
