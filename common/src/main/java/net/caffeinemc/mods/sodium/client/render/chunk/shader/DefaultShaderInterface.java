package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat2v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import net.caffeinemc.mods.sodium.client.util.TextureUtil;
import net.caffeinemc.mods.sodium.mixin.core.render.texture.TextureAtlasAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL32C;

import java.util.EnumMap;
import java.util.Map;

/**
 * A forward-rendering shader program for chunks.
 */
public class DefaultShaderInterface implements ChunkShaderInterface {
    // Direct3D specifies at least 8 bits of sub-texel precision for texture fetches. OpenGL specifies at least
    // 4 bits of sub-texel precision. Most OpenGL-capable graphics are Direct3D-capable as well, so we could
    // *probably* assume 8 bits of precision.
    //
    // However, in practice, this seems to be a complete mess. The rounding behavior for point-filtering seems to
    // be defined inconsistently and depends on the shader compiler and hardware implementation. Apple's GL-on-Metal
    // implementation is the worst of all of them, with a very large epsilon (1.0 / 32.0) being needed to cure
    // texture seams between blocks.
    //
    // Unless we implemented texture filtering in the shader ourselves (i.e. using texelFetch(..)), it is unlikely
    // we could avoid these issues. And that would not help much in the case of linear interpolation across
    // mip layers.
    //
    // So in other words, this constant is the lowest common denominator we found through evaluation on the target
    // hardware. It is rather pessimistic to accommodate for Apple's implementation, but does seem to reliably fix
    // texture seams.
    private static final int SUB_TEXEL_PRECISION_BITS = 5;

    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;
    private final GlUniformFloat2v uniformTexCoordShrink;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public DefaultShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);
        this.uniformTexCoordShrink = context.bindUniform("u_TexCoordShrink", GlUniformFloat2v::new);

        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK, context.bindUniform("u_BlockTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.LIGHT, context.bindUniform("u_LightTex", GlUniformInt::new));

        this.fogShader = options.fog().getFactory().apply(context);
    }

    @Override // the shader interface should not modify pipeline state
    public void setupState() {
        // TODO: Bind to these textures directly rather than using fragile RenderSystem state
        this.bindTexture(ChunkShaderTextureSlot.BLOCK, TextureUtil.getBlockTextureId());
        this.bindTexture(ChunkShaderTextureSlot.LIGHT, TextureUtil.getLightTextureId());

        var textureAtlas = (TextureAtlasAccessor) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);

        // There is a limited amount of sub-texel precision when using hardware texture sampling. The mapped texture
        // area must be "shrunk" by at least one sub-texel to avoid bleed between textures in the atlas.
        this.uniformTexCoordShrink.set(
                (1.0f / textureAtlas.getWidth()) / (1 << SUB_TEXEL_PRECISION_BITS),
                (1.0f / textureAtlas.getHeight()) / (1 << SUB_TEXEL_PRECISION_BITS)
        );

        this.fogShader.setup();
    }

    @Override // the shader interface should not modify pipeline state
    public void resetState() {
        // This is used by alternate implementations.
    }

    @Deprecated(forRemoval = true) // should be handled properly in GFX instead.
    private void bindTexture(ChunkShaderTextureSlot slot, int textureId) {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GlStateManager._bindTexture(textureId);

        var uniform = this.uniformTextures.get(slot);
        uniform.setInt(slot.ordinal());
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
