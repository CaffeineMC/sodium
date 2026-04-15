package net.caffeinemc.mods.sodium.neoforge.level;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelRenderHooks;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.joml.Matrix4f;

public class NeoForgeLevelRenderHooks implements PlatformLevelRenderHooks {
    /**
     * NeoForge 26.1.2.10-beta removed the broken section-origin PoseStack from
     * AddSectionGeometryEvent.SectionRenderingContext (NeoForge PR #3090).
     * Keep one compatibility path here so Sodium can run against both the new
     * constructor and older NeoForge builds without duplicating the render loop.
     */
    private static final SectionRenderingContextFactory SECTION_CONTEXT_FACTORY = resolveSectionContextFactory();

    @Override
    public void runChunkLayerEvents(RenderType renderType, Level level, LevelRenderer levelRenderer, Matrix4f modelMatrix, Matrix4f projectionMatrix, int renderTick, Camera camera, Frustum frustum) {
        //ClientHooks.dispatchRenderStage(RenderLev, level, levelRenderer, modelMatrix, projectionMatrix, renderTick, camera, frustum);
    }

    @Override
    public List<?> retrieveChunkMeshAppenders(Level level, BlockPos origin) {
        return ClientHooks.gatherAdditionalRenderers(origin, level);
    }

    @Override
    public void runChunkMeshAppenders(List<?> renderers, Function<ChunkSectionLayer, VertexConsumer> typeToConsumer, LevelSlice slice, BlockPos origin) {
        AddSectionGeometryEvent.SectionRenderingContext context = SECTION_CONTEXT_FACTORY.create(
                typeToConsumer,
                slice,
                new ModelBlockRenderer(Minecraft.getInstance().options.ambientOcclusion().get(), true, Minecraft.getInstance().getBlockColors()),
                origin
        );
        for (Object o : renderers) {
            ((AddSectionGeometryEvent.AdditionalSectionRenderer) o).render(context);
        }
    }

    private static SectionRenderingContextFactory resolveSectionContextFactory() {
        Constructor<AddSectionGeometryEvent.SectionRenderingContext> currentConstructor = findConstructor(
                Function.class,
                BlockAndTintGetter.class,
                ModelBlockRenderer.class
        );
        if (currentConstructor != null) {
            return (typeToConsumer, slice, blockRenderer, origin) ->
                    instantiate(currentConstructor, typeToConsumer, slice, blockRenderer);
        }

        Constructor<AddSectionGeometryEvent.SectionRenderingContext> legacyConstructor = findConstructor(
                Function.class,
                BlockAndTintGetter.class,
                ModelBlockRenderer.class,
                BlockPos.class
        );
        if (legacyConstructor != null) {
            return (typeToConsumer, slice, blockRenderer, origin) ->
                    instantiate(legacyConstructor, typeToConsumer, slice, blockRenderer, origin);
        }

        throw new IllegalStateException("No compatible SectionRenderingContext constructor found");
    }

    private static AddSectionGeometryEvent.SectionRenderingContext instantiate(
            Constructor<AddSectionGeometryEvent.SectionRenderingContext> constructor,
            Object... arguments
    ) {
        try {
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create SectionRenderingContext", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Constructor<AddSectionGeometryEvent.SectionRenderingContext> findConstructor(Class<?>... parameterTypes) {
        try {
            Constructor<?> constructor = AddSectionGeometryEvent.SectionRenderingContext.class.getConstructor(parameterTypes);
            return (Constructor<AddSectionGeometryEvent.SectionRenderingContext>) constructor;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SectionRenderingContextFactory {
        AddSectionGeometryEvent.SectionRenderingContext create(
                Function<ChunkSectionLayer, VertexConsumer> typeToConsumer,
                BlockAndTintGetter slice,
                ModelBlockRenderer blockRenderer,
                BlockPos origin
        );
    }
}
