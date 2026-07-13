package net.caffeinemc.mods.sodium.mixin.features.render.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.fabric.render.ImprovedItemModelBuilder;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {
    @Unique
    private static final ImprovedItemModelBuilder sodium$INSTANCE = new ImprovedItemModelBuilder();

    @WrapMethod(method = "bakeSideFaces")
    private static void improvedBakeSideFaces(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo, Operation<Void> original) {
        sodium$INSTANCE.bakeSideQuads(builder, interner, materialInfo, modelState, null);
    }
}
