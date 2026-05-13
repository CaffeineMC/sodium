package net.caffeinemc.mods.sodium.mixin.features.render.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ImprovedItemModelBuilder;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {

    @WrapMethod(method = "geometry")
    private UnbakedGeometry improvedBake(Operation<UnbakedGeometry> original) {
        return ImprovedItemModelBuilder::bake;
    }

    @Redirect(
            method = "bakeExtrudedSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/cuboid/ItemModelGenerator;bakeSideFaces(Lnet/minecraft/client/resources/model/geometry/QuadCollection$Builder;Lnet/minecraft/client/resources/model/ModelBaker$Interner;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;)V"
            )
    )
    private static void improvedBakeSideFaces(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo) {
        ImprovedItemModelBuilder.bakeSideQuads(builder, interner, materialInfo, modelState);
    }
}
