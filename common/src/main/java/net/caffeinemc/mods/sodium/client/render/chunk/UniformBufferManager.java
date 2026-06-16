package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.gpu.GPULimits;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.mixin.core.render.texture.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class UniformBufferManager {
    private final MappableRingBuffer uniformData;

    private final GpuBuffer sectionTimeInfo;
    private final GpuBufferSlice.MappedView sectionTimeInfoMap;

    private boolean hasUpdatedThisFrame = false;

    public UniformBufferManager(ClientLevel level, int renderDistance) {
        int renderDistanceDiameter = (2 * renderDistance) + 1;
        int totalVerticalDistance = level.getMaxSectionY() - level.getMinSectionY() + 1;

        int regionsX = (renderDistanceDiameter + (2 * RenderRegion.REGION_WIDTH) - 2) / RenderRegion.REGION_WIDTH;
        int regionsY = (totalVerticalDistance + (2 * RenderRegion.REGION_HEIGHT) - 2) / RenderRegion.REGION_HEIGHT;

        int maxRegions = regionsX * regionsY * regionsX * 2;

        this.uniformData = new MappableRingBuffer(() -> "Sodium uniform buffer", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, 256);

        this.sectionTimeInfo = RenderSystem.getDevice().createBuffer(() -> "Section time info", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE,
                (long) maxRegions * 256L * Integer.BYTES);
        if (RenderSystem.getDevice().getDeviceInfo().features().persistentMapping()) {
            this.sectionTimeInfoMap = this.sectionTimeInfo.map(false, true);
        } else {
            this.sectionTimeInfoMap = null;
        }
    }

    public void prepareFrame() {
        this.hasUpdatedThisFrame = false;
    }

    public void update(ChunkRenderMatrices matrices, FogParameters fogParameters) {
        if (this.hasUpdatedThisFrame) {
            return;
        }
        this.hasUpdatedThisFrame = true;

        this.uniformData.rotate();

        double subTexelPrecision = (1 << GPULimits.getSubTexelPrecisionBits());
        double subTexelOffset = 1.0f / CompactChunkVertex.TEXTURE_MAX_VALUE;

        var textureAtlas = (TextureAtlasAccessor) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);

        try (var data = this.uniformData.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(data.data())
                    .putMat4f(new Matrix4f(matrices.projection()))
                    .putMat4f(new Matrix4f(matrices.modelView()))
                    .putVec4(fogParameters.red(), fogParameters.green(), fogParameters.blue(), fogParameters.alpha())
                    .putVec2(fogParameters.environmentalStart(), fogParameters.environmentalEnd())
                    .putVec2(fogParameters.renderStart(), fogParameters.renderEnd())
                    .putVec2(1.0f / textureAtlas.sodium$getWidth(), 1.0f / textureAtlas.sodium$getHeight())
                    .putVec2((float) (subTexelOffset - (((1.0D / textureAtlas.sodium$getWidth()) / subTexelPrecision))),
                            (float) (subTexelOffset - (((1.0D / textureAtlas.sodium$getHeight()) / subTexelPrecision))))
                    .putFloat((float) (1.0 / (Minecraft.getInstance().options.chunkSectionFadeInTime().get() * 1000.0)))
                    .putInt(Minecraft.getInstance().options.textureFiltering().get() == TextureFilteringMethod.RGSS ? 1 : 0).get();
        }
    }

    public GpuBuffer getUniformBuffer() {
        return this.uniformData.currentBuffer();
    }

    public GpuBuffer getSectionTimeInfo() {
        return this.sectionTimeInfo;
    }

    public void writeMeshTimes(int id, int sectionIndex, int relativeBuiltTime) {
        if ((((id * 256L) + sectionIndex) * 4L) >= this.sectionTimeInfo.size()) throw new IllegalStateException("Overflowed the mesh time buffer at " + id + "x" + sectionIndex);
        if (this.sectionTimeInfoMap != null) {
            MemoryUtil.memPutInt(MemoryUtil.memAddress(this.sectionTimeInfoMap.data()) + (((id * 256L) + sectionIndex) * 4L), relativeBuiltTime);
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = stack.malloc(4);
                data.putInt(relativeBuiltTime);
                data.flip();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.sectionTimeInfo.slice((((id * 256L) + sectionIndex) * 4L), 4), data);
            }
        }
    }

    public void delete() {
        if (this.sectionTimeInfoMap != null) {
            this.sectionTimeInfoMap.close();
        }
        this.sectionTimeInfo.close();
        this.uniformData.close();
    }
}
