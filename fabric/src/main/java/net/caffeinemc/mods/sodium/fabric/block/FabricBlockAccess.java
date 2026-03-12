package net.caffeinemc.mods.sodium.fabric.block;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.model.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class FabricBlockAccess implements PlatformBlockAccess {
    /**
     * Ported from Indigo.
     * Finds mean of per-face shading factors weighted by normal components.
     * Not how light actually works but the vanilla diffuse shading model is a hack to start with
     * and this gives reasonable results for non-cubic surfaces in a vanilla-style renderer.
     */
    private float normalShade(BlockAndTintGetter blockView, float normalX, float normalY, float normalZ, boolean hasShade) {
        float sum = 0;
        float div = 0;

        if (normalX > 0) {
            sum += normalX * getShade(blockView, Direction.EAST, hasShade);
            div += normalX;
        } else if (normalX < 0) {
            sum += -normalX * getShade(blockView, Direction.WEST, hasShade);
            div -= normalX;
        }

        if (normalY > 0) {
            sum += normalY * getShade(blockView, Direction.UP, hasShade);
            div += normalY;
        } else if (normalY < 0) {
            sum += -normalY * getShade(blockView, Direction.DOWN, hasShade);
            div -= normalY;
        }

        if (normalZ > 0) {
            sum += normalZ * getShade(blockView, Direction.SOUTH, hasShade);
            div += normalZ;
        } else if (normalZ < 0) {
            sum += -normalZ * getShade(blockView, Direction.NORTH, hasShade);
            div -= normalZ;
        }

        return sum / div;
    }

    private float getShade(BlockAndTintGetter blockView, Direction direction, boolean hasShade) {
        if (hasShade) {
            return blockView.cardinalLighting().byFace(direction);
        } else {
            return blockView.cardinalLighting().up();
        }
    }

    @Override
    public int getLightEmission(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return state.getLightEmission();
    }

    @Override
    public boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, BlockPos otherPos, Direction facing) {
        return false;
    }

    @Override
    public boolean shouldShowFluidOverlay(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return FluidRenderingRegistry.isBlockTransparent(block.getBlock());
    }

    @Override
    public boolean platformHasBlockData() {
        return true;
    }

    @Override
    public float getNormalVectorShade(ModelQuadView quad, BlockAndTintGetter level, boolean shade) {
        return normalShade(level, NormI8.unpackX(quad.getFaceNormal()), NormI8.unpackY(quad.getFaceNormal()), NormI8.unpackZ(quad.getFaceNormal()), shade);
    }

    @Override
    public AmbientOcclusionMode usesAmbientOcclusion(BlockStateModelPart model, BlockState state, ChunkSectionLayer renderType, BlockAndTintGetter level, BlockPos pos) {
        return model.useAmbientOcclusion() ? AmbientOcclusionMode.DEFAULT : AmbientOcclusionMode.DISABLED;
    }

    @Override
    public boolean shouldBlockEntityGlow(BlockEntity blockEntity, LocalPlayer player) {
        return false;
    }

    @Override
    public boolean shouldOccludeFluid(Direction adjDirection, BlockState adjBlockState, FluidState fluid) {
        return adjBlockState.getFluidState().getType().isSame(fluid.getType());
    }
}
