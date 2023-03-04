package me.jellysquid.mods.sodium.client.model.quad;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.util.math.Direction;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getFaceNormal();

    Direction getCullFace();

    boolean hasShade();
}
