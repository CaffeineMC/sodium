package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.util.SodiumChunkSection;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin implements SodiumChunkSection {
    @Unique
    private SodiumWorldRenderer renderer;

    @Unique
    private ChunkRenderMatrices matrices;

    @Unique
    private double x;

    @Unique
    private double y;

    @Unique
    private double z;

    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    private void sodium$renderGroup(ChunkSectionLayerGroup chunkSectionLayerGroup, GpuSampler gpuSampler, CallbackInfo ci) {
        if (renderer != null) {
            ci.cancel();

            RenderDevice.enterManagedCode();

            try {
                renderer.drawChunkLayer(chunkSectionLayerGroup, matrices, x, y, z, gpuSampler);
            } finally {
                RenderDevice.exitManagedCode();
            }
        }
    }

    @Override
    public void sodium$setRendering(SodiumWorldRenderer renderer, ChunkRenderMatrices matrices, double x, double y, double z) {
        this.renderer = renderer;
        this.matrices = matrices;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
