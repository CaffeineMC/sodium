package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.pipeline.RenderPipelines;
import net.minecraft.resources.Identifier;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private static final Map<ShaderCompilationParameters, RenderPipeline> programs = new Object2ObjectOpenHashMap<>();

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

    protected record ShaderCompilationParameters(TerrainRenderPass pass, OitStage stage, boolean improvedFog, boolean useSphericalFog) {
    }

    protected RenderPipeline compileProgram(TerrainRenderPass pass, @Nullable OitStage stage, ShaderCompilationParameters parameters) {
        RenderPipeline program = programs.get(parameters);

        if (program == null) {
            programs.put(parameters, program = this.createShader(parameters));
        }

        return program;
    }

    private RenderPipeline createShader(ShaderCompilationParameters parameters) {
        List<String> constants = createShaderConstants(parameters);

        RenderPipeline.Snippet snippet = null;

        if (parameters.stage == OitStage.ACCUMULATE) {
            snippet = RenderPipelines.OIT_ACCUMULATE_SNIPPET;
        } else if (parameters.stage == OitStage.TRANSMITTANCE) {
            snippet = RenderPipelines.OIT_TRANSMITTANCE_SNIPPET;
        } else if (parameters.stage == OitStage.DEPTH_BOUNDS) {
            snippet = RenderPipelines.OIT_DEPTH_BOUNDS_SNIPPET;
        }

        var builder = RenderPipeline.builder(snippet == null ? new RenderPipeline.Snippet[0] : new RenderPipeline.Snippet[] { snippet })
                .withBindGroupLayout(BIND_GROUP)
                .withBindGroupLayout(LIGHT_GROUP)
                .withPushConstantSize(DefaultChunkRenderer.PUSH_CONSTANT_RANGE)
                .withLocation(Identifier.fromNamespaceAndPath("sodium", parameters.pass.getPipeline().getLocation().getPath()))
                .withCull(true)
                .withVertexShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))

                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withVertexBinding(0, this.vertexFormat);

        if (parameters.improvedFog && (parameters.stage == null || parameters.stage == OitStage.ACCUMULATE)) {
            builder.withBindGroupLayout(BindGroupLayouts.SKY_CLOUDS_SAMPLER).withShaderDefine("IMPROVED_FOG");
        }

        if (parameters.stage == null) {
            if (parameters.pass.isTranslucent()) {
                builder.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), 0xFFFFFFFF)).withDepthStencilState(DepthStencilState.DEFAULT);
            } else {
                builder.withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(DepthStencilState.DEFAULT);
            }
        }

        for (String s : constants) {
            builder.withShaderDefine(s);
        }

        if (parameters.pass.isTranslucent()) {
            builder.withBindGroupLayout(BindGroupLayouts.WATER_MASK_SAMPLER);
            builder.withShaderDefine("CHECK_WATER_MASK");
            builder.withShaderDefine("ALPHA_CUTOUT", 0.01f);
        } else if (parameters.pass.supportsFragmentDiscard()) {
            builder.withShaderDefine("ALPHA_CUTOUT", 0.5f);
        }

        return builder.build();
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
        ShaderCompilationParameters parameters = new ShaderCompilationParameters(pass, stage, Minecraft.getInstance().gameRenderer.useImprovedTransparency(), SodiumClientMod.options().performance.useSphericalRenderFog);
        this.activeProgram = this.compileProgram(pass, stage, parameters);
    }

    protected void end() {
        this.activeProgram = null;
    }

    @Override
    public void delete() {
    }

}
