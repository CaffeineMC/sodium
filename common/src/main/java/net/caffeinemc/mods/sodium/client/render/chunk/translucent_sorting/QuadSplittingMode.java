package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

import net.caffeinemc.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public enum QuadSplittingMode implements TextProvider {
    OFF("/", 0.0f, "options.off"),
    SAFE("S", 1.0f, "sodium.options.quad_splitting.safe"),
    UNLIMITED("U", Float.POSITIVE_INFINITY, "sodium.options.quad_splitting.unlimited");

    private final String shortName;

    // how much bigger the final geometry is allowed to be compared to the input geometry when performing quad splitting.
    // 0.5f means that the final geometry can be 50% bigger than the input geometry.
    private final float quadSplittingFactor;
    private final Component name;

    QuadSplittingMode(String shortName, float quadSplittingFactor, String name) {
        this.shortName = shortName;
        this.quadSplittingFactor = quadSplittingFactor;
        this.name = Component.translatable(name);
    }

    @Override
    public Component getLocalizedName() {
        return this.name;
    }

    public String getShortName() {
        return this.shortName;
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
