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
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class UniformBufferManager {
    private static final int GLOBAL_UNIFORM_SIZE = 256;
    private static final int INITIAL_GLOBAL_UNIFORM_CAPACITY = 8;

    private final DynamicUniformStorage<GlobalUniforms> uniformStorage;
    private GpuBufferSlice uniformData;

    private final GpuBuffer sectionTimeInfo;
    private final GpuBufferSlice.MappedView sectionTimeInfoMap;

    private boolean hasUpdatedThisFrame = false;

    public UniformBufferManager(ClientLevel level, int renderDistance) {
        int renderDistanceDiameter = (2 * renderDistance) + 1;
        int totalVerticalDistance = level.getMaxSectionY() - level.getMinSectionY() + 1;

        int regionsX = (renderDistanceDiameter + (2 * RenderRegion.REGION_WIDTH) - 2) / RenderRegion.REGION_WIDTH;
        int regionsY = (totalVerticalDistance + (2 * RenderRegion.REGION_HEIGHT) - 2) / RenderRegion.REGION_HEIGHT;

        int maxRegions = regionsX * regionsY * regionsX * 2;

        this.uniformStorage = new DynamicUniformStorage<>("Sodium terrain uniforms", GLOBAL_UNIFORM_SIZE, INITIAL_GLOBAL_UNIFORM_CAPACITY);

        this.sectionTimeInfo = RenderSystem.getDevice().createBuffer(() -> "Section time info", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE,
                (long) maxRegions * 256L * Integer.BYTES);

        ByteBuffer copy = MemoryUtil.memAlloc(maxRegions * 256 * Integer.BYTES);
        MemoryUtil.memSet(copy, 0xFFFFFFFF);
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(sectionTimeInfo.slice(), copy);
        MemoryUtil.memFree(copy);

        if (RenderSystem.getDevice().getDeviceInfo().features().persistentMapping()) {
            this.sectionTimeInfoMap = this.sectionTimeInfo.map(false, true);
        } else {
            this.sectionTimeInfoMap = null;
        }
    }

    public void prepareFrame() {
        this.hasUpdatedThisFrame = false;
    }

    public void endFrame() {
        this.uniformStorage.endFrame();
        this.uniformData = null;
    }

    public void update(ChunkRenderMatrices matrices, FogParameters fogParameters) {
        if (this.hasUpdatedThisFrame) {
            return;
        }
        this.hasUpdatedThisFrame = true;

        double subTexelPrecision = (1 << GPULimits.getSubTexelPrecisionBits());
        double subTexelOffset = 1.0f / CompactChunkVertex.TEXTURE_MAX_VALUE;

        var textureAtlas = (TextureAtlasAccessor) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);

        this.uniformData = this.uniformStorage.writeUniform(new GlobalUniforms(
                new Matrix4f(matrices.projection()),
                new Matrix4f(matrices.modelView()),
                fogParameters.red(), fogParameters.green(), fogParameters.blue(), fogParameters.alpha(),
                fogParameters.environmentalStart(), fogParameters.environmentalEnd(),
                fogParameters.renderStart(), fogParameters.renderEnd(),
                1.0f / textureAtlas.sodium$getWidth(), 1.0f / textureAtlas.sodium$getHeight(),
                (float) (subTexelOffset - (((1.0D / textureAtlas.sodium$getWidth()) / subTexelPrecision))),
                (float) (subTexelOffset - (((1.0D / textureAtlas.sodium$getHeight()) / subTexelPrecision))),
                (float) (1.0 / (Minecraft.getInstance().options.chunkSectionFadeInTime().get() * 1000.0)),
                Minecraft.getInstance().options.textureFiltering().get() == TextureFilteringMethod.RGSS ? 1 : 0
        ));
    }

    public GpuBufferSlice getUniformBuffer() {
        if (this.uniformData == null) {
            throw new IllegalStateException("Global terrain uniforms have not been updated");
        }

        return this.uniformData;
    }

    public GpuBuffer getSectionTimeInfo() {
        return this.sectionTimeInfo;
    }

    private static final ByteBuffer INVALID = MemoryUtil.memAlloc(1024);

    static {
        MemoryUtil.memSet(INVALID, 0xFFFFFFFF);
    }

    public void clearRegionTimes(int id) {
        if (this.sectionTimeInfoMap != null) {
            // 256 * 4 bytes = 1024
            MemoryUtil.memSet(MemoryUtil.memAddress(this.sectionTimeInfoMap.data()) + (id * 1024L), 0xFFFFFFFF, 1024L);
        } else {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.sectionTimeInfo.slice(id * 1024L, 1024L), INVALID);
        }
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
        this.uniformStorage.close();
    }

    private record GlobalUniforms(
            Matrix4f projection,
            Matrix4f modelView,
            float fogRed,
            float fogGreen,
            float fogBlue,
            float fogAlpha,
            float environmentalFogStart,
            float environmentalFogEnd,
            float renderFogStart,
            float renderFogEnd,
            float textureAtlasWidthInverse,
            float textureAtlasHeightInverse,
            float subTexelOffsetX,
            float subTexelOffsetY,
            float fadeInFactor,
            int useRgbaTextureFiltering
    ) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(@NonNull ByteBuffer byteBuffer) {
            Std140Builder.intoBuffer(byteBuffer)
                    .putMat4f(this.projection)
                    .putMat4f(this.modelView)
                    .putVec4(this.fogRed, this.fogGreen, this.fogBlue, this.fogAlpha)
                    .putVec2(this.environmentalFogStart, this.environmentalFogEnd)
                    .putVec2(this.renderFogStart, this.renderFogEnd)
                    .putVec2(this.textureAtlasWidthInverse, this.textureAtlasHeightInverse)
                    .putVec2(this.subTexelOffsetX, this.subTexelOffsetY)
                    .putFloat(this.fadeInFactor)
                    .putInt(this.useRgbaTextureFiltering)
                    .get();
        }
    }
}
