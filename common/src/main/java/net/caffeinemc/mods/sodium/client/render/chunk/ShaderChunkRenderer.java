package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.mixin.core.CommandEncoderAccessor;
import net.caffeinemc.mods.sodium.mixin.core.GlCommandEncoderAccessor;
import net.caffeinemc.mods.sodium.mixin.core.GpuDeviceAccessor;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private static final Map<TerrainRenderPass, RenderPipeline> programs = new Object2ObjectOpenHashMap<>();
    private static final BindGroupLayout BIND_GROUP = BindGroupLayout.builder()
            .withSampler("u_LightTex")
            .withSampler("u_BlockTex")
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("u_SectionTimeInfo", UniformType.TEXEL_BUFFER, GpuFormat.R32_SINT).build();

    protected final ChunkVertexType vertexType;
    protected final VertexFormat vertexFormat;

    protected final RenderDevice device;

    protected RenderPipeline activeProgram;

    public ShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getVertexFormat();
    }

    protected RenderPipeline compileProgram(TerrainRenderPass options) {
        RenderPipeline program = programs.get(options);

        if (program == null) {
            programs.put(options, program = this.createShader("blocks/block_layer_opaque", options));
        }

        return program;
    }

    private RenderPipeline createShader(String path, TerrainRenderPass options) {
        List<String> constants = createShaderConstants(options);

        var builder = RenderPipeline.builder()
                .withBindGroupLayout(BIND_GROUP)
                .withLocation(Identifier.fromNamespaceAndPath("sodium", options.getPipeline().getLocation().getPath()))
                .withCull(true)
                .withVertexShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withVertexBinding(0, vertexFormat);

        if (options.isTranslucent()) {
            builder.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, 0xFFFFFFFF));
        } else {
            builder.withColorTargetState(ColorTargetState.DEFAULT);
        }

        for (String s : constants) {
            builder.withShaderDefine(s);
        }

        return builder.build();
    }

    private static List<String> createShaderConstants(TerrainRenderPass pass) {
        List<String> defines = new ArrayList<>();

        if (pass.supportsFragmentDiscard()) {
            defines.add("USE_FRAGMENT_DISCARD");
        }

        defines.add("USE_VERTEX_COMPRESSION"); // TODO: allow compact vertex format to be disabled
        defines.add("USE_FOG");

        return defines;
    }

    protected void begin(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler) {
        this.activeProgram = this.compileProgram(pass);
    }

    protected void end(TerrainRenderPass pass) {
        this.activeProgram = null;
    }

    @Override
    public void delete(CommandList commandList) {
    }

}
