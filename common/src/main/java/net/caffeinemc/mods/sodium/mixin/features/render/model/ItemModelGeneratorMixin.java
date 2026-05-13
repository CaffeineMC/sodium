package net.caffeinemc.mods.sodium.mixin.features.render.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ImprovedItemModelBuilder;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ItemModelGenerator.class)
public class ItemModelGeneratorMixin {

    @WrapMethod(method = "createSideElements")
    private static List<BlockElement> improvedCreateSideElements(SpriteContents spriteContents, String layer, int index, Operation<List<BlockElement>> original) {
        return ImprovedItemModelBuilder.bakeSideQuads(spriteContents, layer, index);
    }
}