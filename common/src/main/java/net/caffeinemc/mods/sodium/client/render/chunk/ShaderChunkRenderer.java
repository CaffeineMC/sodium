package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private static final Map<TerrainRenderPass, RenderPipeline> programs = new Object2ObjectOpenHashMap<>();
    public static final BindGroupLayout BIND_GROUP = BindGroupLayout.builder()
            .withSampler("u_LightTex")
            .withSampler("u_BlockTex")
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("u_SectionTimeInfo", UniformType.TEXEL_BUFFER, GpuFormat.R32_SINT).build();

    protected final ChunkVertexType vertexType;
    protected final VertexFormat vertexFormat;

    protected RenderPipeline activeProgram;

    public ShaderChunkRenderer(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getVertexFormat();
    }

    protected RenderPipeline compileProgram(TerrainRenderPass pass) {
        RenderPipeline program = programs.get(pass);

        if (program == null) {
            programs.put(pass, program = this.createShader("blocks/block_layer_opaque", pass));
        }

        return program;
    }

    private RenderPipeline createShader(String path, TerrainRenderPass pass) {
        List<String> constants = createShaderConstants(pass);

        var builder = RenderPipeline.builder()
                .withBindGroupLayout(BIND_GROUP)
                .withLocation(Identifier.fromNamespaceAndPath("sodium", pass.getPipeline().getLocation().getPath()))
                .withCull(true)
                .withVertexShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withVertexBinding(0, this.vertexFormat);

        if (pass.isTranslucent()) {
            builder.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), GpuFormat.RGBA8_UNORM, 0xFFFFFFFF));
        } else {
            builder.withColorTargetState(ColorTargetState.DEFAULT);
        }

        for (String s : constants) {
            builder.withShaderDefine(s);
        }

        if (pass.isTranslucent()) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.01f);
        } else if (pass.supportsFragmentDiscard()) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5f);
        }

        return builder.build();
    }

    private static List<String> createShaderConstants(TerrainRenderPass pass) {
        List<String> defines = new ArrayList<>();

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
    public void delete() {
    }

}
