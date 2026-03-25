package net.caffeinemc.mods.sodium.mixin.features.render.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ImprovedItemModelBuilder;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @WrapOperation(method = "discoverModelDependencies", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelDiscovery;addSpecialModel(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/model/UnbakedModel;)V"))
    private static void replaceItemModelGenerator(ModelDiscovery instance, Identifier identifier, UnbakedModel model, Operation<Void> original) {
        original.call(instance, identifier, new ImprovedItemModelBuilder());
    }
}
