package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformUnsignedInt;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL46C;

import java.util.EnumMap;
import java.util.Map;

/**
 * A forward-rendering shader program for chunks.
 */
public class DefaultShaderInterface implements ChunkShaderInterface {
    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    private final GlUniformBlock uniformGlobals;
    private final GlUniformFloat3v regionUniform;
    private final GlUniformInt timeUniform;
    private final GlUniformUnsignedInt idUniform;

    public DefaultShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK, context.bindUniform("u_BlockTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.LIGHT, context.bindUniform("u_LightTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.SECTION, context.bindUniform("u_SectionTimeInfo", GlUniformInt::new));

        this.regionUniform = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);
        this.timeUniform = context.bindUniform("u_CurrentTime", GlUniformInt::new);
        this.idUniform = context.bindUniform("u_RegionID", GlUniformUnsignedInt::new);

        this.uniformGlobals = context.bindUniformBlock("u_Globals", 0);
    }

    @Override // the shader interface should not modify pipeline state
    public void setupState(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler, GpuBufferSlice uniformData, GlTexelBuffer sectionTimeInfo) {
        this.bindTexture(ChunkShaderTextureSlot.BLOCK, pass.getAtlas(), terrainSampler);
        this.bindTexture(ChunkShaderTextureSlot.LIGHT, Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + ChunkShaderTextureSlot.SECTION.ordinal());
        GL46C.glBindTexture(GL46C.GL_TEXTURE_BUFFER, sectionTimeInfo.handle());
        var uniform = this.uniformTextures.get(ChunkShaderTextureSlot.SECTION);
        uniform.setInt(ChunkShaderTextureSlot.SECTION.ordinal());

        this.uniformGlobals.bindBufferRange(uniformData);
    }

    @Override // the shader interface should not modify pipeline state
    public void resetState() {
        // This is used by alternate implementations.
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    @Override
    public void setRegionData(CameraTransform camera, RenderRegion region) {
        float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
        float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
        float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);

        this.regionUniform.set(x, y, z);
        this.timeUniform.set(Math.toIntExact(System.currentTimeMillis() - region.getCreationTime()));
        this.idUniform.set(region.getId());
    }

    @Deprecated(forRemoval = true) // should be handled properly in GFX instead.
    private void bindTexture(ChunkShaderTextureSlot slot, GpuTextureView textureView, GpuSampler sampler) {
        GlTexture tex = (GlTexture) textureView.texture();
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GlStateManager._bindTexture(tex.glId());
        GlStateManager._texParameter(GL32C.GL_TEXTURE_2D, 33084, textureView.baseMipLevel());
        GlStateManager._texParameter(GL32C.GL_TEXTURE_2D, 33085, textureView.baseMipLevel() + textureView.mipLevels() - 1);
        GL33C.glBindSampler(slot.ordinal(), ((GlSampler) sampler).getId());

        var uniform = this.uniformTextures.get(slot);
        uniform.setInt(slot.ordinal());
    }
}
