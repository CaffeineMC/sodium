package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.resources.Identifier;

import java.util.*;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.jspecify.annotations.Nullable;


public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private static final Map<TerrainRenderPass, RenderPipeline> programs = new Object2ObjectOpenHashMap<>();
    private static final Map<TerrainRenderPass, OitPipelineSet> oitPrograms = new Object2ObjectOpenHashMap<>();
    public static final BindGroupLayout BIND_GROUP = BindGroupLayout.builder()
            .withUniform("u_BlockTex", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("u_SectionTimeInfo", UniformType.TEXEL_BUFFER, GpuFormat.R32_SINT).build();
    public static final BindGroupLayout LIGHT_GROUP = BindGroupLayout.builder()
            .withUniform("u_LightTex", UniformType.COMBINED_IMAGE_SAMPLER)
            .build();

    protected final ChunkVertexType vertexType;
    protected final VertexFormat vertexFormat;

    protected RenderPipeline activeProgram;

    public ShaderChunkRenderer(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getVertexFormat();
    }

    protected RenderPipeline compileProgram(TerrainRenderPass pass, @Nullable OitStage stage) {
        if (stage == null) {
            RenderPipeline program = programs.get(pass);

            if (program == null) {
                programs.put(pass, program = this.createShader("blocks/block_layer_opaque", pass));
            }

            return program;
        } else {
            OitPipelineSet program = oitPrograms.get(pass);

            if (program == null) {
                oitPrograms.put(pass, program = this.createOITShader("blocks/block_layer_opaque", pass, stage));
            }

            return program.getPipeline(stage);
        }
    }

    private RenderPipeline createShader(String path, TerrainRenderPass pass) {
        List<String> constants = createShaderConstants(pass);

        var builder = RenderPipeline.builder()
                .withBindGroupLayout(BIND_GROUP)
                .withBindGroupLayout(LIGHT_GROUP)
                .withPushConstantSize(DefaultChunkRenderer.PUSH_CONSTANT_RANGE)
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

    private OitPipelineSet createOITShader(String path, TerrainRenderPass pass, OitStage stage) {
        List<String> constants = createShaderConstants(pass);

        var builder = RenderPipeline.builder()
                .withBindGroupLayout(BIND_GROUP)
                .withPushConstantSize(DefaultChunkRenderer.PUSH_CONSTANT_RANGE)
                .withLocation(Identifier.fromNamespaceAndPath("sodium", pass.getPipeline().getLocation().getPath()))
                .withCull(true)
                .withVertexShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withVertexBinding(0, this.vertexFormat);

        for (String s : constants) {
            builder.withShaderDefine(s);
        }

        if (pass.isTranslucent()) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.01f);
        } else if (pass.supportsFragmentDiscard()) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5f);
        }

        return OitPipelineSet.builder(
                "sodium_terrain", builder).withAccumulateModifier(i -> i.withBindGroupLayout(LIGHT_GROUP)).build();
    }

    private static List<String> createShaderConstants(TerrainRenderPass pass) {
        List<String> defines = new ArrayList<>();

        defines.add("USE_VERTEX_COMPRESSION"); // TODO: allow compact vertex format to be disabled
        defines.add("USE_FOG");

        return defines;
    }

    protected void begin(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler, @Nullable OitStage stage) {
        this.activeProgram = this.compileProgram(pass, stage);
    }

    protected void end(TerrainRenderPass pass) {
        this.activeProgram = null;
    }

    @Override
    public void delete() {
    }

}
