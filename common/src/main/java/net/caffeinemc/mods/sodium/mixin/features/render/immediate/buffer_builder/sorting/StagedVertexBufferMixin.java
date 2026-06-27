package net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_builder.sorting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.joml.Intersectionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;


@Mixin(StagedVertexBuffer.class)
public class StagedVertexBufferMixin {
    @WrapOperation(
            method = "decodeSortingPoints",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/MeshData;decodeQuadCentroids(Ljava/nio/ByteBuffer;ILcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/vertex/CompactVectorArray;I)V"))
    private static void sodium$selectClosestSortingPoints(ByteBuffer vertexBuffer, int vertexCount, VertexFormat format, CompactVectorArray output, int outputIndex, Operation<Void> original, @Local(argsOnly = true) StagedVertexBuffer.Draw draw) {
        if (SodiumClientMod.options().quality.useClosestPointEntitySort &&
                ((DrawAccessor) draw).getQuadSorting() == VertexSorting.DISTANCE_TO_ORIGIN) {
            VertexFormatElement positionElement = format.getElement("Position");
            if (positionElement == null) {
                throw new IllegalArgumentException("Cannot identify quad points with no position element");
            }

            int positionOffset = vertexBuffer.position() + positionElement.offset();
            int vertexStride = format.getVertexSize();
            int quadStride = vertexStride * 4;
            int quadCount = vertexCount / 4;

            final var scratch = new Vector3f();

            for (int i = 0; i < quadCount; i++) {
                int offset0 = i * quadStride + positionOffset;
                int offset1 = offset0 + vertexStride;
                int offset2 = offset0 + vertexStride * 2;

                float v0x = vertexBuffer.getFloat(offset0 + 0);
                float v0y = vertexBuffer.getFloat(offset0 + 4);
                float v0z = vertexBuffer.getFloat(offset0 + 8);

                float v1x = vertexBuffer.getFloat(offset1 + 0);
                float v1y = vertexBuffer.getFloat(offset1 + 4);
                float v1z = vertexBuffer.getFloat(offset1 + 8);

                float v2x = vertexBuffer.getFloat(offset2 + 0);
                float v2y = vertexBuffer.getFloat(offset2 + 4);
                float v2z = vertexBuffer.getFloat(offset2 + 8);

                // Closest point on the quad (assuming it's flat and rectangular) to the camera at the view-space origin,
                // which may not be the centroid. The first point must be the corner shared by the other two edges of the
                // rectangle.
                Intersectionf.findClosestPointOnRectangle(
                        v1x, v1y, v1z,
                        v0x, v0y, v0z,
                        v2x, v2y, v2z,
                        0.0f, 0.0f, 0.0f,
                        scratch);

                output.set(outputIndex + i, scratch.x, scratch.y, scratch.z);
            }
        } else {
            original.call(vertexBuffer, vertexCount, format, output, outputIndex);
        }
    }
}
