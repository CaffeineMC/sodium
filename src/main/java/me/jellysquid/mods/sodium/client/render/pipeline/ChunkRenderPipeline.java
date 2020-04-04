package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Random;

public class ChunkRenderPipeline {
    private final ChunkBlockRenderPipeline blockRenderer;
    private final ChunkFluidRenderPipeline fluidRenderer;

    private final BlockModels models;

    private final Random random = new XoRoShiRoRandom();

    public ChunkRenderPipeline(MinecraftClient client, ChunkSlice world) {
        this.blockRenderer = new ChunkBlockRenderPipeline(client, world);
        this.fluidRenderer = new ChunkFluidRenderPipeline();

        this.models = client.getBakedModelManager().getBlockModels();
    }

    public boolean renderBlock(ChunkMeshInfo.Builder meshInfo, BlockState state, BlockPos pos, BlockRenderView world, Vector3f offset, BufferBuilder builder, boolean cull) {
        BlockRenderType type = state.getRenderType();

        if (type != BlockRenderType.MODEL) {
            return false;
        }

        return this.blockRenderer.renderModel(meshInfo, world, this.models.getModel(state), state, pos, offset, builder, cull, this.random, state.getRenderingSeed(pos));
    }

    public void renderFluid(ChunkMeshInfo.Builder meshInfo, BlockPos.Mutable pos, ChunkSlice region, BufferBuilder builder, FluidState fluidState) {
        this.fluidRenderer.render(meshInfo, region, pos, builder, fluidState);
    }
}
