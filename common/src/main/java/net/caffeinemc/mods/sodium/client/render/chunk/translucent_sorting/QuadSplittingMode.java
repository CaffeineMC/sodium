package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

import net.caffeinemc.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public enum QuadSplittingMode implements TextProvider {
    OFF(0.0f, "options.off"),
    SAFE(1.0f, "sodium.options.quad_splitting.safe"),
    UNLIMITED(Float.POSITIVE_INFINITY, "sodium.options.quad_splitting.unlimited");

    // how much bigger the final geometry is allowed to be compared to the input geometry when performing quad splitting.
    // 0.5f means that the final geometry can be 50% bigger than the input geometry.
    private final float quadSplittingFactor;
    private final Component name;

    QuadSplittingMode(float quadSplittingFactor, String name) {
        this.quadSplittingFactor = quadSplittingFactor;
        this.name = Component.translatable(name);
    }

    @Override
    public Component getLocalizedName() {
        return this.name;
    }

    public boolean allowsSplitting() {
        return this != OFF;
    }

    public int getMaxExtraQuads(int baseQuadCount) {
        if (Float.isInfinite(this.quadSplittingFactor)) {
            return Integer.MAX_VALUE;
        }
        return Mth.ceil(baseQuadCount * this.quadSplittingFactor);
    }
}
