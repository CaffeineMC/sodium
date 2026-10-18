package net.caffeinemc.mods.sodium.client.model.quad;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing sodium$getNormalFace();

    int sodium$getFaceNormal();

    boolean sodium$hasShade();

    boolean sodium$hasAO();
}
