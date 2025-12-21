package net.caffeinemc.mods.sodium.api.util;

/**
 * Provides some utilities for packing and unpacking color components from packed integer colors in ARGB format. This
 * packed format is used by most of Minecraft, but special care must be taken to pack it into ABGR format before passing
 * it to OpenGL attributes.
 * <p>
 * | 32        | 24        | 16        | 8          |
 * | 0110 1100 | 0110 1100 | 0110 1100 | 0110 1100  |
 * | Alpha     | Red       | Green     | Blue       |
 */
public class ColorARGB implements ColorU8 {
    private static final int ALPHA_COMPONENT_OFFSET = 24;
    private static final int RED_COMPONENT_OFFSET = 16;
    private static final int GREEN_COMPONENT_OFFSET = 8;
    private static final int BLUE_COMPONENT_OFFSET = 0;

    private static final int RED_COMPONENT_MASK     = COMPONENT_MASK << RED_COMPONENT_OFFSET;
    private static final int GREEN_COMPONENT_MASK   = COMPONENT_MASK << GREEN_COMPONENT_OFFSET;
    private static final int BLUE_COMPONENT_MASK    = COMPONENT_MASK << BLUE_COMPONENT_OFFSET;
    private static final int ALPHA_COMPONENT_MASK   = COMPONENT_MASK << ALPHA_COMPONENT_OFFSET;

    /**
     * Packs the specified color components into big-endian format for consumption by OpenGL.
     *
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     * @param a The alpha component of the color
     */
    public static int pack(int r, int g, int b, int a) {
        return (a & COMPONENT_MASK) << ALPHA_COMPONENT_OFFSET |
                (r & COMPONENT_MASK) << RED_COMPONENT_OFFSET |
                (g & COMPONENT_MASK) << GREEN_COMPONENT_OFFSET |
                (b & COMPONENT_MASK) << BLUE_COMPONENT_OFFSET;
    }

    /**
     * Packs the specified color components into big-endian format for consumption by OpenGL. The alpha
     * channel is fully opaque.
     *
     * @param r The red component of the color
     * @param g The green component of the color
     * @param b The blue component of the color
     */
    public static int pack(int r, int g, int b) {
        return pack(r, g, b, (1 << ColorU8.COMPONENT_BITS) - 1);
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackAlpha(int color) {
        return color >> ALPHA_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The red color component in the range of 0..255
     */
    public static int unpackRed(int color) {
        return color >> RED_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The green color component in the range of 0..255
     */
    public static int unpackGreen(int color) {
        return color >> GREEN_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * @param color The packed 32-bit ARGB color to unpack
     * @return The blue color component in the range of 0..255
     */
    public static int unpackBlue(int color) {
        return color >> BLUE_COMPONENT_OFFSET & COMPONENT_MASK;
    }

    /**
     * Swizzles from ARGB format into ABGR format, replacing the alpha component with {@param alpha}.
     */
    public static int toABGR(int color, int alpha) {
        // shl(ARGB, 8) -> RGB0
        // or(RGB0, 000A) -> RGBA
        return Integer.reverseBytes(color << 8 | alpha);
    }

    /**
     * Swizzles from ARGB format into ABGR format, replacing the alpha component with {@param alpha}. The alpha
     * component is mapped from [0.0, 1.0] to [0, 255].
     */
    public static int toABGR(int color, float alpha) {
        return toABGR(color, ColorU8.normalizedFloatToByte(alpha));
    }

    /**
     * Swizzles from ARGB format into ABGR format.
     */
    public static int toABGR(int color) {
        // rotateLeft(ARGB, 8) -> RGBA
        // reverseBytes(RGBA) -> ABGR
        return Integer.reverseBytes(Integer.rotateLeft(color, 8));
    }

    /**
     * Swizzles from ABGR format into ARGB format.
     */
    public static int fromABGR(int color) {
        // reverseBytes(ABGR) -> RGBA
        // rotateRight(RGBA, 8) -> ARGB
        return Integer.rotateRight(Integer.reverseBytes(color), 8);
    }

    /**
     * Packs the specified color components into ARGB format.
     *
     * @param rgb   The red/green/blue component of the color
     * @param alpha The alpha component of the color
     */
    public static int withAlpha(int rgb, int alpha) {
        return (alpha << ALPHA_COMPONENT_OFFSET) | (rgb & ~(COMPONENT_MASK << ALPHA_COMPONENT_OFFSET));
    }

    /**
     * Replaces the alpha component of the specified color with the alpha component of another color.
     *
     * @param color      The packed 32-bit ARGB color to modify
     * @param alphaColor The packed 32-bit ARGB color to take the alpha component from
     * @return The modified packed 32-bit ARGB color
     */
    public static int transferAlpha(int color, int alphaColor) {
        return withAlpha(color, unpackAlpha(alphaColor));
    }

    /**
     * Multiplies the RGB components of the color with the provided factor. The alpha component is not modified.
     *
     * @param color  The packed 32-bit ABGR color to be multiplied
     * @param factor The darkening factor (in the range of 0..255) to multiply with
     */
    public static int mulRGB(int color, int factor) {
        return (ColorMixer.mul(color, factor) & ~ALPHA_COMPONENT_MASK) | (color & ALPHA_COMPONENT_MASK);
    }

    /**
     * See {@link #mulRGB(int, int)}. This function is identical, but it accepts a float in [0.0, 1.0] instead, which
     * is then mapped to [0, 255].
     *
     * @param color  The packed 32-bit ABGR color to be multiplied
     * @param factor The darkening factor (in the range of 0.0..1.0) to multiply with
     */
    public static int mulRGB(int color, float factor) {
        return mulRGB(color, ColorU8.normalizedFloatToByte(factor));
    }

    /**
     * Converts the specified packed ARGB color into HSV color space.
     * <p>
     * Used with permission from patbox.
     *
     * @param color The packed 32-bit ARGB color to convert
     * @return An array containing the hue, saturation and value components in [0,1] in that order.
     */
    public static float[] toHSV(int color) {
        float r = (float) unpackRed(color) / 255;
        float g = (float) unpackGreen(color) / 255;
        float b = (float) unpackBlue(color) / 255;

        float cmax = Math.max(r, Math.max(g, b));
        float cmin = Math.min(r, Math.min(g, b));
        float diff = cmax - cmin;
        float h = -1, s = -1;

        if (cmax == cmin) {
            h = 0;
        } else if (cmax == r) {
            h = (0.1666f * ((g - b) / diff) + 1) % 1;
        } else if (cmax == g) {
            h = (0.1666f * ((b - r) / diff) + 0.333f) % 1;
        } else if (cmax == b) {
            h = (0.1666f * ((r - g) / diff) + 0.666f) % 1;
        }
        if (cmax == 0) {
            s = 0;
        } else {
            s = (diff / cmax);
        }

        return new float[] { h, s, cmax };
    }

    private static int pack(float r, float g, float b) {
        return pack(
                ColorU8.normalizedFloatToByte(r),
                ColorU8.normalizedFloatToByte(g),
                ColorU8.normalizedFloatToByte(b)
        );
    }

    /**
     * Converts the specified HSV color components into a packed ARGB color.
     * <p>
     * Used with permission from patbox
     *
     * @param hue        The hue component in [0,1]
     * @param saturation The saturation component in [0,1]
     * @param value      The value component in [0,1]
     * @return The packed 32-bit ARGB color
     */
    public static int fromHSV(float hue, float saturation, float value) {
        int h = (int) (hue * 6) % 6;
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        return switch (h) {
            case 0 -> pack(value, t, p);
            case 1 -> pack(q, value, p);
            case 2 -> pack(p, value, t);
            case 3 -> pack(p, q, value);
            case 4 -> pack(t, p, value);
            case 5 -> pack(value, p, q);
            default -> 0;
        };
    }

    public static int fromHSV(float[] hsv) {
        return fromHSV(hsv[0], hsv[1], hsv[2]);
    }
}
