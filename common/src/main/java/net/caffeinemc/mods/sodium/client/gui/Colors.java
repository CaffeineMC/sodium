package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.util.Mth;

// colors in ARGB format
public class Colors {
    public static final int THEME = 0xFF94E4D3;
    public static final int THEME_LIGHTER = 0xFFCCFDEE;
    public static final int THEME_DARKER = 0xFF7A9E9E;
    public static final int FOREGROUND = 0xFFFFFFFF;
    public static final int FOREGROUND_DISABLED = 0xFFAAAAAA;

    public static final int BACKGROUND_LIGHT = 0x40000000;
    public static final int BACKGROUND_MEDIUM = 0x60000000;
    public static final int BACKGROUND_HOVER = 0xE0000000;
    public static final int BACKGROUND_OVERLAY = 0xEA000000;
    public static final int BACKGROUND_DEFAULT = 0x90000000;
    public static final int BACKGROUND_DARKER = 0xB0000000;
    public static final int BACKGROUND_HIGHLIGHT = 0x08FFFFFF;

    public static final int BUTTON_BORDER = 0x8000FFEE;

    private static final float LIGHTEN_FACTOR = 0.3f;
    private static final float DARKEN_FACTOR = -0.23f;

    public static int darken(int color) {
        return adjust(color, DARKEN_FACTOR);
    }

    public static int lighten(int color) {
        return adjust(color, LIGHTEN_FACTOR);
    }

    public static int adjust(int color, float factor) {
        float[] hsv = ColorARGB.toHSV(color);
        var s = Mth.clamp(hsv[1] * (1 - Math.abs(factor)), 0, 1);
        var b = Mth.clamp(hsv[2] * (1 + factor), 0, 1);
        return ColorARGB.transferAlpha(ColorARGB.fromHSV(hsv[0], s, b), color);
    }

    public static int constrainColorHSV(int color, float minSaturation, float minBrightness) {
        float[] hsv = ColorARGB.toHSV(color);
        hsv[1] = Math.max(hsv[1], minSaturation);
        hsv[2] = Math.max(hsv[2], minBrightness);
        return ColorARGB.fromHSV(hsv);
    }
}
