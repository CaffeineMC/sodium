package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private static final Map<TerrainRenderPass, RenderPipeline> programs = new Object2ObjectOpenHashMap<>();
    private static final Map<TerrainRenderPass, OitPipelineSet> oitPrograms = new Object2ObjectOpenHashMap<>();
    private static ShaderCompilationParameters lastCompilationParameters = null;

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

    protected record ShaderCompilationParameters(boolean useSphericalFog) {
    }

    protected RenderPipeline compileProgram(TerrainRenderPass pass, @Nullable OitStage stage, ShaderCompilationParameters parameters) {
        if (!parameters.equals(lastCompilationParameters)) {
            programs.clear();
            oitPrograms.clear();
            lastCompilationParameters = parameters;
        }

        if (stage == null) {
            RenderPipeline program = programs.get(pass);

            if (program == null) {
                programs.put(pass, program = this.createShader(pass, parameters));
            }

            return program;
        } else {
            OitPipelineSet program = oitPrograms.get(pass);

            if (program == null) {
                oitPrograms.put(pass, program = this.createOITShader(pass, parameters));
            }

            return program.getPipeline(stage);
        }
    }

    private RenderPipeline createShader(TerrainRenderPass pass, ShaderCompilationParameters parameters) {
        List<String> constants = createShaderConstants(parameters);

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

    private OitPipelineSet createOITShader(TerrainRenderPass pass, ShaderCompilationParameters parameters) {
        List<String> constants = createShaderConstants(parameters);

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

    private static List<String> createShaderConstants(ShaderCompilationParameters parameters) {
        List<String> defines = new ArrayList<>();

        defines.add("USE_FOG");
        if (parameters.useSphericalFog()) {
            defines.add("USE_SPHERICAL_FOG");
        }

        return defines;
    }

    protected void begin(TerrainRenderPass pass, @Nullable OitStage stage) {
        ShaderCompilationParameters parameters = new ShaderCompilationParameters(SodiumClientMod.options().performance.useSphericalRenderFog);
        this.activeProgram = this.compileProgram(pass, stage, parameters);
    }

    protected void end() {
        this.activeProgram = null;
    }

    @Override
    public void delete() {
    }

}
