package net.caffeinemc.mods.sodium.fabric.level;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelAccess;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

public class FabricLevelAccess implements PlatformLevelAccess {
    @Override
    public FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        return new FluidRendererImpl(colorRegistry, lightPipelineProvider);
    }

    @Override
    public boolean tryRenderFluid() {
        return FluidRendererImpl.tryRenderFluid();
    }

    @Override
    public void runChunkLayerEvents(RenderType renderLayer, LevelRenderer levelRenderer, Matrix4f modelMatrix, Matrix4f projectionMatrix, int ticks, Camera mainCamera, Frustum cullingFrustum) {

    }

    @Override
    public List<?> getExtraRenderers(Level level, BlockPos origin) {
        return List.of();
    }

    @Override
    public void renderAdditionalRenderers(List<?> renderers, Function<RenderType, VertexConsumer> typeToConsumer, LevelSlice slice) {

    }

    @Override
    public @Nullable Object getLightManager(LevelChunk chunk, SectionPos pos) {
        return null;
    }
}
