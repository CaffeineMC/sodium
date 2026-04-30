package net.caffeinemc.mods.sodium.client.model.light;

import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.flat.FlatFluidLightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline;

import java.util.EnumMap;

public class LightPipelineProvider {
    private final EnumMap<LightMode, LightPipeline> lighters = new EnumMap<>(LightMode.class);
    private final FlatFluidLightPipeline flatFluidLighter;

    public LightPipelineProvider(LightDataAccess cache) {
        this.lighters.put(LightMode.SMOOTH, new SmoothLightPipeline(cache));
        this.lighters.put(LightMode.FLAT, new FlatLightPipeline(cache));
        this.flatFluidLighter = new FlatFluidLightPipeline(cache);
    }

    public LightPipeline getLighter(LightMode type) {
        LightPipeline pipeline = this.lighters.get(type);

        if (pipeline == null) {
            throw new NullPointerException("No lighter exists for mode: " + type.name());
        }

        return pipeline;
    }

    public FlatFluidLightPipeline getFlatFluidLighter() {
        return this.flatFluidLighter;
    }
}
