package net.caffeinemc.mods.sodium.client.model.light.flat;

import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;

import java.util.Arrays;

import static net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess.unpackFC;

/**
 * A flat light pipeline variant for fluid faces. It mirrors the behavior of
 * {@link net.minecraft.client.renderer.block.FluidRenderer#getLightCoords}, which
 * computes a face's lightmap as the maximum of the block at {@code pos} and the
 * block above it. Without this, fluids appear darker than vanilla, see <a href="https://github.com/CaffeineMC/sodium/issues/3454">this issue.</a>
 */
public class FlatFluidLightPipeline extends FlatLightPipeline {
    public FlatFluidLightPipeline(LightDataAccess lightCache) {
        super(lightCache);
    }

    @Override
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade, boolean enhanced) {
        // this first part is a reduced implementation of FlatLightPipeline
        if (cullFace != null) {
            Arrays.fill(out.br, this.getShade(this.lightCache.getLevel(), lightFace, shade));
        } else {
            int flags = quad.getFlags();
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && unpackFC(this.lightCache.get(pos)))) {
                Arrays.fill(out.br, this.getShade(this.lightCache.getLevel(), lightFace, shade));
            } else {
                Arrays.fill(out.br, enhanced ? PlatformBlockAccess.getInstance().getNormalVectorShade(quad, this.lightCache.getLevel(), shade) : this.getShade(this.lightCache.getLevel(), lightFace, shade));
            }
        }

        int self = LightDataAccess.getEmissiveLightmap(this.lightCache.get(pos));
        int neighbor = LightDataAccess.getEmissiveLightmap(this.lightCache.get(pos.getX(), pos.getY() + 1, pos.getZ()));

        int merged = LightCoordsUtil.pack(
                Math.max(LightCoordsUtil.block(self), LightCoordsUtil.block(neighbor)),
                Math.max(LightCoordsUtil.sky(self), LightCoordsUtil.sky(neighbor)));

        Arrays.fill(out.lm, merged);
    }
}
