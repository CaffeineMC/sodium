package net.caffeinemc.mods.sodium.client.util;

import org.joml.Matrix4fc;

public interface GameRendererStorage extends FogStorage {
    Matrix4fc sodium$getProjectionMatrix();
}
