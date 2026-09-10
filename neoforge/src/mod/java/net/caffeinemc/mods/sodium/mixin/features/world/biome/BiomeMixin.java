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
    private boolean sodium$hasCustomDryFoliageColor;

    @Unique
    private int sodium$customDryFoliageColor;

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

        var grassColorOverride = this.sodium$cachedSpecialEffects.grassColorOverride();
        if (grassColorOverride.isPresent()) {
            this.sodium$hasCustomGrassColor = true;
            this.sodium$customGrassColor = grassColorOverride.get();
        } else {
            this.sodium$hasCustomGrassColor = false;
        }

        var foliageColorOverride = this.sodium$cachedSpecialEffects.foliageColorOverride();
        if (foliageColorOverride.isPresent()) {
            this.sodium$hasCustomFoliageColor = true;
            this.sodium$customFoliageColor = foliageColorOverride.get();
        } else {
            this.sodium$hasCustomFoliageColor = false;
        }

        var dryFoliageColorOverride = this.sodium$cachedSpecialEffects.dryFoliageColorOverride();
        if (dryFoliageColorOverride.isPresent()) {
            this.sodium$hasCustomDryFoliageColor = true;
            this.sodium$customDryFoliageColor = dryFoliageColorOverride.get();
        } else {
            this.sodium$hasCustomDryFoliageColor = false;
        }

        this.sodium$defaultColorIndex = this.sodium$getDefaultColorIndex();
    }

    // Grass Color

    /**
     * @author AViewFromTheTop
     * @reason Avoid unnecessary pointer de-references and allocations. Based on JellySquid's previous overwrite of {@code getGrassColor}.
     */
    @Overwrite
    private int getBaseGrassColor() {
        if (this.getModifiedSpecialEffects() != this.sodium$cachedSpecialEffects) {
            this.sodium$setupColors();
        }

        if (this.sodium$hasCustomGrassColor) {
            return this.sodium$customGrassColor;
        } else {
            return BiomeColorMaps.getGrassColor(this.sodium$defaultColorIndex);
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
        return this.sodium$cachedSpecialEffects;
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

    // Dry Foliage Color

    /**
     * @author AViewFromTheTop (Copied from JellySquid)
     * @reason Avoid unnecessary pointer de-references and allocations
     */
    @Overwrite
    public int getDryFoliageColor() {
        if (this.getModifiedSpecialEffects() != this.sodium$cachedSpecialEffects) {
            this.sodium$setupColors();
        }

        int color;

        if (this.sodium$hasCustomDryFoliageColor) {
            color = this.sodium$customDryFoliageColor;
        } else {
            color = BiomeColorMaps.getDryFoliageColor(this.sodium$defaultColorIndex);
        }

        return color;
    }

    @Unique
    private int sodium$getDefaultColorIndex() {
        double temperature = Mth.clamp(this.climateSettings.temperature(), 0.0F, 1.0F);
        double downfall = Mth.clamp(this.climateSettings.downfall(), 0.0F, 1.0F);

        return BiomeColorMaps.getIndex(temperature, downfall);
    }
}
