package net.caffeinemc.mods.sodium.mixin.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.render.frapi.render.OrderedSubmitNodeCollectorExtension;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshView;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin implements OrderedSubmitNodeCollectorExtension {
    @Shadow
    public abstract SubmitNodeCollection order(int i);

    @Override
    public void fabric_submitItem(PoseStack matrices, ItemDisplayContext displayContext, int light, int overlay, int outlineColors, int[] tintLayers, List<BakedQuad> quads, RenderType renderLayer, ItemStackRenderState.FoilType foilType, MeshView mesh) {
        SubmitNodeCollection queue = order(0);

        if (queue instanceof OrderedSubmitNodeCollectorExtension access) {
            access.fabric_submitItem(matrices, displayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, foilType, mesh);
        } else {
            queue.submitItem(matrices, displayContext, light, overlay, outlineColors, tintLayers, quads, renderLayer, foilType);
        }
    }
}
