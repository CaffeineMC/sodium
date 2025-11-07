package net.caffeinemc.mods.sodium.mixin.features.render.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadParticleRenderState.class)
public abstract class QuadParticleRenderStateMixin {
    @Unique
    private static final Quaternionf TEMP_QUAT = new Quaternionf();

    @Unique
    private static final Vector3f TEMP_VECTOR = new Vector3f();

    /**
     * @reason Build vertex data using the left and up vectors to avoid quaternion calculations
     * @author MoePus
     */
    @Inject(method = "renderRotatedQuad", at = @At("HEAD"), cancellable = true)
    protected void render(VertexConsumer vertexConsumer,
                          float x, float y, float z,
                          float quatX, float quatY, float quatZ, float quatW, // what? why?
                          float size,
                          float u0, float u1, float v0, float v1,
                          int color, int light, CallbackInfo ci) {
        final var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        TEMP_QUAT.set(quatX, quatY, quatZ, quatW);

        this.sodium$emitVertices(writer, x, y, z, size, u0, u1, v0, v1, ColorARGB.toABGR(color), light, TEMP_QUAT);
    }

    @Unique
    private void sodium$emitVertices(VertexBufferWriter writer, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int color, int light, Quaternionf quaternion) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ParticleVertex.STRIDE);
            long ptr = buffer;

            TEMP_VECTOR.set(1.0F, -1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);

            ParticleVertex.put(ptr, TEMP_VECTOR.x, TEMP_VECTOR.y, TEMP_VECTOR.z, u1, v1, color, light);
            ptr += ParticleVertex.STRIDE;

            TEMP_VECTOR.set(1.0F, 1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);

            ParticleVertex.put(ptr, TEMP_VECTOR.x, TEMP_VECTOR.y, TEMP_VECTOR.z, u1, v0, color, light);
            ptr += ParticleVertex.STRIDE;

            TEMP_VECTOR.set(-1.0F, 1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);

            ParticleVertex.put(ptr, TEMP_VECTOR.x, TEMP_VECTOR.y, TEMP_VECTOR.z, u0, v0, color, light);
            ptr += ParticleVertex.STRIDE;

            TEMP_VECTOR.set(-1.0F, -1.0F, 0.0F).rotate(quaternion).mul(size).add(x, y, z);

            ParticleVertex.put(ptr, TEMP_VECTOR.x, TEMP_VECTOR.y, TEMP_VECTOR.z, u0, v1, color, light);
            ptr += ParticleVertex.STRIDE;

            writer.push(stack, buffer, 4, ParticleVertex.FORMAT);
        }
    }
}
