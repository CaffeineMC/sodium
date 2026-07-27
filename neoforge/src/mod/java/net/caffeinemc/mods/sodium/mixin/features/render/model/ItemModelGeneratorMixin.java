package net.caffeinemc.mods.sodium.mixin.features.render.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.neoforge.render.ImprovedItemModelBuilder;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {
    @Unique
    private static final ImprovedItemModelBuilder sodium$INSTANCE = new ImprovedItemModelBuilder();

    @WrapMethod(method = "bakeSideFaces(Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;Lnet/minecraft/client/resources/model/ModelBaker$Interner;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;Lnet/neoforged/neoforge/client/model/ExtraFaceData;)V")
    private static void improvedBakeSideFaces(QuadCollection.Builder builder,
                                              ModelBaker.Interner interner,
                                              ModelState modelState,
                                              BakedQuad.MaterialInfo materialInfo,
                                              ExtraFaceData faceData,
                                              Operation<Void> original) {
        sodium$INSTANCE.bakeSideQuads(builder, interner, materialInfo, modelState, faceData);
    }
}
