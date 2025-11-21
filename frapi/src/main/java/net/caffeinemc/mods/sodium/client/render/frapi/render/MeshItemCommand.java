package net.caffeinemc.mods.sodium.client.render.frapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshView;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;

public record MeshItemCommand(PoseStack.Pose positionMatrix, ItemDisplayContext displayContext, int lightCoords,
                              int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads,
                              RenderType renderType, ItemStackRenderState.FoilType glintType, MeshView mesh) {
}