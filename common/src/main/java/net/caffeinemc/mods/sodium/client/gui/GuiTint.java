package net.caffeinemc.mods.sodium.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;

// On 1.21.1 tinting works by setting the shader color and blend mode, drawing, and then resetting.
public final class GuiTint {
    private GuiTint() {
    }

    public static void withTint(int argb, Runnable draw) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            draw.run();
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    public static void noTint(Runnable draw) {
        withTint(0xFFFFFFFF, draw);
    }
}
