package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.pipeline.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SodiumTerrainPipelines {
    private static final Map<ShaderCompilationParameters, RenderPipeline> programs = new Object2ObjectOpenHashMap<>();

    public static final BindGroupLayout BIND_GROUP = BindGroupLayout.builder()
            .withUniform("u_BlockTex", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("u_Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("u_SectionTimeInfo", UniformType.TEXEL_BUFFER, GpuFormat.R32_SINT).build();
    public static final BindGroupLayout LIGHT_GROUP = BindGroupLayout.builder()
            .withUniform("u_LightTex", UniformType.COMBINED_IMAGE_SAMPLER)
            .build();

    public static RenderPipeline compileProgram(ShaderCompilationParameters parameters) {
        RenderPipeline program = programs.get(parameters);

        if (program == null) {
            programs.put(parameters, program = createShader(parameters));
        }

        return program;
    }

    private static RenderPipeline createShader(ShaderCompilationParameters parameters) {
        List<String> constants = createShaderConstants(parameters);

        RenderPipeline.Snippet snippet = null;

        if (parameters.stage() == OitStage.ACCUMULATE) {
            snippet = RenderPipelines.OIT_ACCUMULATE_SNIPPET;
        } else if (parameters.stage() == OitStage.TRANSMITTANCE) {
            snippet = RenderPipelines.OIT_TRANSMITTANCE_SNIPPET;
        } else if (parameters.stage() == OitStage.DEPTH_BOUNDS) {
            snippet = RenderPipelines.OIT_DEPTH_BOUNDS_SNIPPET;
        }

        var builder = RenderPipeline.builder(snippet == null ? new RenderPipeline.Snippet[0] : new RenderPipeline.Snippet[] { snippet })
                .withBindGroupLayout(BIND_GROUP)
                .withBindGroupLayout(LIGHT_GROUP)
                .withPushConstantSize(DefaultChunkRenderer.PUSH_CONSTANT_RANGE)
                .withLocation(Identifier.fromNamespaceAndPath("sodium", parameters.pass().getPipeline().getLocation().getPath()))
                .withCull(true)
                .withVertexShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque"))

                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withVertexBinding(0, parameters.vertexType().getVertexFormat());

        if (parameters.improvedFog() && (parameters.stage() == null || parameters.stage() == OitStage.ACCUMULATE)) {
            builder.withBindGroupLayout(BindGroupLayouts.SKY_CLOUDS_SAMPLER).withShaderDefine("IMPROVED_FOG");
        }

        if (parameters.stage() == null) {
            if (parameters.pass().isTranslucent()) {
                builder.withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), 0xFFFFFFFF)).withDepthStencilState(DepthStencilState.DEFAULT);
            } else {
                builder.withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(DepthStencilState.DEFAULT);
            }
        }

        for (String s : constants) {
            builder.withShaderDefine(s);
        }

        if (parameters.pass().isTranslucent()) {
            builder.withBindGroupLayout(BindGroupLayouts.WATER_MASK_SAMPLER);
            builder.withShaderDefine("CHECK_WATER_MASK");
            builder.withShaderDefine("ALPHA_CUTOUT", 0.01f);
        } else if (parameters.pass().supportsFragmentDiscard()) {
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
}
