package net.caffeinemc.mods.sodium.client.render.frapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.renderer.v1.render.ItemRenderTypeGetter;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

public interface OrderedSubmitNodeCollectorExtension {
    void fabric_submitItem(
            PoseStack matrices,
            ItemDisplayContext displayContext,
            int light,
            int overlay,
            int outlineColors,
            int[] tintLayers,
            List<BakedQuad> quads,
            RenderType renderLayer,
            ItemStackRenderState.FoilType glintType,
            MeshView mesh,
            ItemRenderTypeGetter renderTypeGetter
    );
}
