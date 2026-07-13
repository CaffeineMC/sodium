package net.caffeinemc.mods.sodium.fabric.render;

import com.mojang.math.Quadrant;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ImprovedItemModelBuilderBase;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

public class ImprovedItemModelBuilder extends ImprovedItemModelBuilderBase<Void> {
    @Override
    public BakedQuad bakeQuad(ModelBaker.Interner interner, Vector3fc from, Vector3fc to, CuboidFace.UVs uvs, BakedQuad.MaterialInfo materialInfo, Direction normal, ModelState modelState, Void extraFaceData) {
        return FaceBakery.bakeQuad(interner, from, to, uvs, Quadrant.R0, materialInfo, normal, modelState, null);
    }
}
