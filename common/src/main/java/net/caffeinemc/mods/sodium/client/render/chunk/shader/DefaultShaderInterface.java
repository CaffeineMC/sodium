package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.*;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.mixin.core.render.texture.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.util.EnumMap;
import java.util.Map;

/**
 * A forward-rendering shader program for chunks.
 */
public class DefaultShaderInterface implements ChunkShaderInterface {
    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;
    private final GlUniformFloat2v uniformTexCoordShrink;
    private final GlUniformFloat2v uniformTexelSize;
    private final GlUniformBool uniformRGSS;
    private final GlUniformInt uniformCurrentTime;
    private final GlUniformFloat uniformFadePeriod;

    private final GlUniformBlock uniformChunkData;

    // The fog shader component used by this program in order to set up the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public DefaultShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);
        this.uniformTexCoordShrink = context.bindUniform("u_TexCoordShrink", GlUniformFloat2v::new);
        this.uniformTexelSize = context.bindUniform("u_TexelSize", GlUniformFloat2v::new);
        this.uniformRGSS = context.bindUniform("u_UseRGSS", GlUniformBool::new);

        this.uniformCurrentTime = context.bindUniform("u_CurrentTime", GlUniformInt::new);
        this.uniformFadePeriod = context.bindUniform("u_FadePeriodInv", GlUniformFloat::new);

        this.uniformChunkData = context.bindUniformBlock("ChunkData", 0);

        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK, context.bindUniform("u_BlockTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.LIGHT, context.bindUniform("u_LightTex", GlUniformInt::new));

        this.fogShader = options.fog().getFactory().apply(context);
    }

    @Override // the shader interface should not modify pipeline state
    public void setupState(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler) {
        this.bindTexture(ChunkShaderTextureSlot.BLOCK, pass.getAtlas(), terrainSampler);
        this.bindTexture(ChunkShaderTextureSlot.LIGHT, Minecraft.getInstance().gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

        var textureAtlas = (TextureAtlasAccessor) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);

        // There is a limited amount of sub-texel precision when using hardware texture sampling. The mapped texture
        // area must be "shrunk" by at least one sub-texel to avoid bleed between textures in the atlas. And since we
        // offset texture coordinates in the vertex format by one texel, we also need to undo that here.
        double subTexelPrecision = (1 << GLRenderDevice.INSTANCE.getSubTexelPrecisionBits());
        double subTexelOffset = 1.0f / CompactChunkVertex.TEXTURE_MAX_VALUE;

        this.uniformTexCoordShrink.set(
                (float) (subTexelOffset - (((1.0D / textureAtlas.getWidth()) / subTexelPrecision))),
                (float) (subTexelOffset - (((1.0D / textureAtlas.getHeight()) / subTexelPrecision)))
        );

        this.uniformTexelSize.set(
                1.0f / textureAtlas.getWidth(),
                1.0f / textureAtlas.getHeight()
        );

        uniformFadePeriod.setFloat((float) (1.0 / (Minecraft.getInstance().options.chunkSectionFadeInTime().get() * 1000.0))); // this is in seconds!

        this.uniformRGSS.setBool(Minecraft.getInstance().options.textureFiltering().get() == TextureFilteringMethod.RGSS);

        this.fogShader.setup(parameters);
    }

    @Override // the shader interface should not modify pipeline state
    public void resetState() {
        // This is used by alternate implementations.
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

    @Override
    public void setChunkData(GlBuffer data, int time) {
        uniformChunkData.bindBuffer(data);
        uniformCurrentTime.set(time);
    }

    @Override
    public void setProjectionMatrix(Matrix4fc matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    @Override
    public void setModelViewMatrix(Matrix4fc matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    @Override
    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);
    }
}
