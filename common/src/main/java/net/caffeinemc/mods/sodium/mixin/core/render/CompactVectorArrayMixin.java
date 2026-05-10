package net.caffeinemc.mods.sodium.mixin.core.render;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Vanilla's {@link CompactVectorArray#getZ(int)} reads index `3 * i + 1` instead of `3 * i + 2` which returns the y component instead of the z component. This breaks vertex sorting in some situations, as seen <a href="https://github.com/CaffeineMC/sodium/issues/3442">here</a>.
 * <p>
 * Fixed in Minecraft 26.2, remove this Mixin when porting to that version.
 */
@Mixin(CompactVectorArray.class)
public class CompactVectorArrayMixin {
    @ModifyConstant(method = "getZ", constant = @Constant(intValue = 1))
    private int sodium$fixGetZ(int original) {
        return 2;
    }
}
