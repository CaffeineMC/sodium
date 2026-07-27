package net.caffeinemc.mods.sodium.mixin.features.world.biome;

import net.caffeinemc.mods.sodium.client.world.biome.BiomeColorMaps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Shadow
    public abstract BiomeSpecialEffects getModifiedSpecialEffects();

    @Shadow
    @Final
    private Biome.ClimateSettings climateSettings;

    @Unique
    private boolean sodium$hasCustomGrassColor;

    @Unique
    private int sodium$customGrassColor;

    @Unique
    private boolean sodium$hasCustomFoliageColor;

    @Unique
    private int sodium$customFoliageColor;

    @Unique
    private int sodium$defaultColorIndex;

    @Unique
    private BiomeSpecialEffects sodium$cachedSpecialEffects;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.sodium$setupColors();
    }

    @Unique
    private void sodium$setupColors() {
        this.sodium$cachedSpecialEffects = this.getModifiedSpecialEffects();

        var grassColor = this.sodium$cachedSpecialEffects.grassColorOverride();

        if (grassColor.isPresent()) {
            this.sodium$hasCustomGrassColor = true;
            this.sodium$customGrassColor = grassColor.get();
        } else {
            this.sodium$hasCustomGrassColor = false;
        }

        var foliageColor = this.sodium$cachedSpecialEffects.foliageColorOverride();

        if (foliageColor.isPresent()) {
            this.sodium$hasCustomFoliageColor = true;
            this.sodium$customFoliageColor = foliageColor.get();
        } else {
            this.sodium$hasCustomFoliageColor = false;
        }

        this.sodium$defaultColorIndex = this.sodium$getDefaultColorIndex();
    }

    /**
     * @author JellySquid
     * @reason Avoid unnecessary pointer de-references and allocations
     */
    @Overwrite
    public int getGrassColor(double x, double z) {
        if (this.getModifiedSpecialEffects() != this.sodium$cachedSpecialEffects) {
            this.sodium$setupColors();
        }

        int color;

        if (this.sodium$hasCustomGrassColor) {
            color = this.sodium$customGrassColor;
        } else {
            color = BiomeColorMaps.getGrassColor(this.sodium$defaultColorIndex);
        }

        var modifier = this.sodium$cachedSpecialEffects.grassColorModifier();

        if (modifier != BiomeSpecialEffects.GrassColorModifier.NONE) {
            color = modifier.modifyColor(x, z, color);
        }

        return color;
    }

    /**
     * @author JellySquid
     * @reason Avoid unnecessary pointer de-references and allocations
     */
    @Overwrite
    public int getFoliageColor() {
        if (this.getModifiedSpecialEffects() != this.sodium$cachedSpecialEffects) {
            this.sodium$setupColors();
        }

        int color;

        if (this.sodium$hasCustomFoliageColor) {
            color = this.sodium$customFoliageColor;
        } else {
            color = BiomeColorMaps.getFoliageColor(this.sodium$defaultColorIndex);
        }

        return color;
    }

    @Unique
    private int sodium$getDefaultColorIndex() {
        double temperature = Mth.clamp(this.climateSettings.temperature(), 0.0F, 1.0F);
        double humidity = Mth.clamp(this.climateSettings.downfall(), 0.0F, 1.0F);

        return BiomeColorMaps.getIndex(temperature, humidity);
    }
}
