package net.caffeinemc.mods.sodium.mixin.features.world.biome;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.world.biome.BiomeColorMaps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Shadow
    @Final
    private Biome.ClimateSettings climateSettings;

    @Shadow
    @Final
    private BiomeSpecialEffects specialEffects;
    @Unique
    private boolean hasCustomGrassColor;

    @Unique
    private int customGrassColor;

    @Unique
    private boolean hasCustomFoliageColor;

    @Unique
    private int customFoliageColor;

    @Unique
    private boolean hasCustomDryFoliageColor;

    @Unique
    private int customDryFoliageColor;

    @Unique
    private int defaultColorIndex;

    @Unique
    private BiomeSpecialEffects cachedSpecialEffects;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.setupColors();
    }

    @Unique
    private void setupColors() {
        this.cachedSpecialEffects = this.specialEffects;

        var grassColorOverride = this.cachedSpecialEffects.grassColorOverride();
        if (grassColorOverride.isPresent()) {
            this.hasCustomGrassColor = true;
            this.customGrassColor = grassColorOverride.get();
        } else {
            this.hasCustomGrassColor = false;
        }

        var foliageColorOverride = this.cachedSpecialEffects.foliageColorOverride();
        if (foliageColorOverride.isPresent()) {
            this.hasCustomFoliageColor = true;
            this.customFoliageColor = foliageColorOverride.get();
        } else {
            this.hasCustomFoliageColor = false;
        }

        var dryFoliageColorOverride = this.cachedSpecialEffects.dryFoliageColorOverride();
        if (dryFoliageColorOverride.isPresent()) {
            this.hasCustomDryFoliageColor = true;
            this.customDryFoliageColor = dryFoliageColorOverride.get();
        } else {
            this.hasCustomDryFoliageColor = false;
        }

        this.defaultColorIndex = this.getDefaultColorIndex();
    }

    // Grass Color

    /**
     * @author AViewFromTheTop
     * @reason Avoid unnecessary pointer de-references and allocations. Based on JellySquid's previous overwrite of {@code getGrassColor}.
     */
    @Overwrite
    private int getBaseGrassColor() {
        if (this.specialEffects != this.cachedSpecialEffects) {
            this.setupColors();
        }

        if (this.hasCustomGrassColor) {
           return this.customGrassColor;
        } else {
           return BiomeColorMaps.getGrassColor(this.defaultColorIndex);
        }
    }

    @ModifyExpressionValue(
            method = "getGrassColor",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/biome/Biome;specialEffects:Lnet/minecraft/world/level/biome/BiomeSpecialEffects;",
                    opcode = Opcodes.GETFIELD
            )
    )
    public BiomeSpecialEffects useCachedSpecialEffects(BiomeSpecialEffects original) {
        return this.cachedSpecialEffects;
    }

    @WrapOperation(
            method = "getGrassColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/BiomeSpecialEffects$GrassColorModifier;modifyColor(DDI)I"
            )
    )
    public int modifyGrassColor(BiomeSpecialEffects.GrassColorModifier modifier, double x, double z, int baseColor, Operation<Integer> original) {
        if (modifier == BiomeSpecialEffects.GrassColorModifier.NONE) {
            return baseColor;
        }

        return original.call(modifier, x, z, baseColor);
    }

    // Foliage Color

    /**
     * @author JellySquid
     * @reason Avoid unnecessary pointer de-references and allocations
     */
    @Overwrite
    public int getFoliageColor() {
        if (this.specialEffects != this.cachedSpecialEffects) {
            this.setupColors();
        }

        int color;

        if (this.hasCustomFoliageColor) {
            color = this.customFoliageColor;
        } else {
            color = BiomeColorMaps.getFoliageColor(this.defaultColorIndex);
        }

        return color;
    }

    // Dry Foliage Color

    /**
     * @author AViewFromTheTop (Copied from JellySquid)
     * @reason Avoid unnecessary pointer de-references and allocations
     */
    @Overwrite
    public int getDryFoliageColor() {
        if (this.specialEffects != this.cachedSpecialEffects) {
            this.setupColors();
        }

        int color;

        if (this.hasCustomDryFoliageColor) {
            color = this.customDryFoliageColor;
        } else {
            color = BiomeColorMaps.getDryFoliageColor(this.defaultColorIndex);
        }

        return color;
    }

    @Unique
    private int getDefaultColorIndex() {
        double temperature = Mth.clamp(this.climateSettings.temperature(), 0.0F, 1.0F);
        double downfall = Mth.clamp(this.climateSettings.downfall(), 0.0F, 1.0F);

        return BiomeColorMaps.getIndex(temperature, downfall);
    }
}
