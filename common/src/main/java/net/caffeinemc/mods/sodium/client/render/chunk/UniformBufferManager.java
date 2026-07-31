package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferTarget;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.mixin.core.GlBufferAccessor;
import net.caffeinemc.mods.sodium.mixin.core.render.texture.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class UniformBufferManager {
    private static final int GLOBAL_UNIFORM_SIZE = 256;
    private static final int INITIAL_GLOBAL_UNIFORM_CAPACITY = 8;
    private static final int TIME_BUFFER_SIZE_PER_REGION = RenderRegion.REGION_SIZE * Integer.BYTES;

    private final DynamicUniformStorage<GlobalUniforms> uniformStorage;
    private GpuBufferSlice uniformData;

    private GpuBuffer sectionTimeInfo;
    private GlTexelBuffer sectionTimeInfoTexture;
    private GpuBuffer.MappedView sectionTimeInfoMap;

    private boolean hasUpdatedThisFrame = false;

    public UniformBufferManager(ClientLevel level, int renderDistance) {
        int renderDistanceDiameter = (2 * renderDistance) + 1;
        int totalVerticalDistance = level.getMaxSectionY() - level.getMinSectionY() + 1;

        int regionsX = (renderDistanceDiameter + (2 * RenderRegion.REGION_WIDTH) - 2) / RenderRegion.REGION_WIDTH;
        int regionsY = (totalVerticalDistance + (2 * RenderRegion.REGION_HEIGHT) - 2) / RenderRegion.REGION_HEIGHT;

        int maxRegions = regionsX * regionsY * regionsX * 2;

        this.uniformStorage = new DynamicUniformStorage<>("Sodium terrain uniforms", GLOBAL_UNIFORM_SIZE, INITIAL_GLOBAL_UNIFORM_CAPACITY);

        this.sectionTimeInfo = RenderSystem.getDevice().createBuffer(() -> "Section time info",
                GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE,
                (long) maxRegions * TIME_BUFFER_SIZE_PER_REGION);

        this.sectionTimeInfoTexture = new GlTexelBuffer(((GlBufferAccessor) this.sectionTimeInfo).sodium$getHandle(), GL46C.GL_R32I);

        if (MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            this.sectionTimeInfoMap = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.sectionTimeInfo, false, true);
            MemoryUtil.memSet(sectionTimeInfoMap.data(), 0xFFFFFFFF);
        } else {
            try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.sectionTimeInfo, false, true)) {
                MemoryUtil.memSet(map.data(), 0xFFFFFFFF);
            }
            this.sectionTimeInfoMap = null;
        }

        onSectionTimeInfoWritten();
    }

    // Somehow, some mods cause more regions to be loaded than are mathematically possible. This fixes that case, while logging it happened.
    public void resizeIfNeeded(int newRequiredIndex) {
        var newSize = Math.max(this.sectionTimeInfo.size() * 2, (long) newRequiredIndex * TIME_BUFFER_SIZE_PER_REGION);

        // Note that either something has gone wrong, or a mod is interfering weirdly
        SodiumClientMod.logger().warn("Had to resize the section time buffer to be bigger ({} bytes), this should never be possible in Vanilla.", newSize);

        if (sectionTimeInfoMap != null) sectionTimeInfoMap.close();
        if (sectionTimeInfoTexture != null) sectionTimeInfoTexture.destroy();

        var oldBuffer = sectionTimeInfo;
        sectionTimeInfo = RenderSystem.getDevice().createBuffer(() -> "Section time info", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE, newSize);

        RenderSystem.getDevice().createCommandEncoder().copyToBuffer(oldBuffer.slice(), sectionTimeInfo.slice(0, oldBuffer.size()));

        if (MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            this.sectionTimeInfoMap = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.sectionTimeInfo, false, true);
            MemoryUtil.memSet(MemoryUtil.memAddress(sectionTimeInfoMap.data()) + oldBuffer.size(), 0xFFFFFFFF, sectionTimeInfo.size() - oldBuffer.size());
        } else {
            try (var mapping = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.sectionTimeInfo, false, true)) {
                MemoryUtil.memSet(MemoryUtil.memAddress(mapping.data()) + oldBuffer.size(), 0xFFFFFFFF, sectionTimeInfo.size() - oldBuffer.size());
            }
        }

        this.sectionTimeInfoTexture = new GlTexelBuffer(((GlBufferAccessor) this.sectionTimeInfo).sodium$getHandle(), GL46C.GL_R32I);

        oldBuffer.close(); // defers

        onSectionTimeInfoWritten();
    }

    // When we bind a buffer to this target because we're writing without using a mapped buffer, then we're modifying the state without GlStateTracker being part of the process. This method notifies it so it doesn't skip re-binding the buffer later after the state has become invalid.
    private static void onSectionTimeInfoWritten() {
        RenderDevice.INSTANCE.invalidateBufferBinding(GlBufferTarget.COPY_WRITE_BUFFER);
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

        double subTexelPrecision = (1 << RenderDevice.INSTANCE.getSubTexelPrecisionBits());
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

    public GlTexelBuffer getSectionTimeInfoTexture() {
        return this.sectionTimeInfoTexture;
    }

    private static final ByteBuffer INVALID = MemoryUtil.memAlloc(1024);

    static {
        MemoryUtil.memSet(INVALID, 0xFFFFFFFF);
    }

    public void clearRegionTimes(int id) {
        long regionTimesOffset = (long) id * TIME_BUFFER_SIZE_PER_REGION;

        if (this.sectionTimeInfoMap != null) {
            long pRegionTimes = MemoryUtil.memAddress(this.sectionTimeInfoMap.data()) + regionTimesOffset;
            MemoryUtil.memSet(pRegionTimes, 0xFFFFFFFF, TIME_BUFFER_SIZE_PER_REGION);
        } else {
            GpuBufferSlice slice = this.sectionTimeInfo.slice(regionTimesOffset, TIME_BUFFER_SIZE_PER_REGION);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(slice, INVALID);
            onSectionTimeInfoWritten();
        }
    }

    public void writeMeshTimes(int id, int sectionIndex, int relativeBuiltTime) {
        long sectionTimeOffset = ((long) id * RenderRegion.REGION_SIZE + sectionIndex) * Integer.BYTES;

        if (sectionTimeOffset >= this.sectionTimeInfo.size()) {
            resizeIfNeeded(id);
        }

        if (this.sectionTimeInfoMap != null) {
            long pSectionTime = MemoryUtil.memAddress(this.sectionTimeInfoMap.data()) + sectionTimeOffset;
            MemoryUtil.memPutInt(pSectionTime, relativeBuiltTime);
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = stack.malloc(Integer.BYTES);
                data.putInt(relativeBuiltTime);
                data.flip();
                GpuBufferSlice slice = this.sectionTimeInfo.slice(sectionTimeOffset, Integer.BYTES);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(slice, data);
            }
            onSectionTimeInfoWritten();
        }
    }

    public void delete() {
        if (this.sectionTimeInfoMap != null) {
            this.sectionTimeInfoMap.close();
        }
        this.sectionTimeInfoTexture.destroy();
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
