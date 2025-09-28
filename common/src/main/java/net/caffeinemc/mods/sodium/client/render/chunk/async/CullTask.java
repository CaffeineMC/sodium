package net.caffeinemc.mods.sodium.client.render.chunk.async;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;

import java.util.Collection;

public abstract class CullTask<T> extends AsyncRenderTask<T> {
    protected final OcclusionCuller occlusionCuller;
    protected final boolean useOcclusionCulling;
    private Collection<RenderSection> presentPatches;

    protected CullTask(Viewport viewport, float buildDistance, int frame, OcclusionCuller occlusionCuller, boolean useOcclusionCulling) {
        super(viewport, buildDistance, frame);
        this.occlusionCuller = occlusionCuller;
        this.useOcclusionCulling = useOcclusionCulling;
    }

    public abstract CullType getCullType();

    @Override
    public void registerPresentPatches(Collection<RenderSection> presentPatches) {
        this.presentPatches = presentPatches;
    }

    protected void applyPresentPatches(SectionTree result) {
        if (this.presentPatches == null) {
            return;
        }

        for (var section : this.presentPatches) {
            var x = section.getChunkX();
            var y = section.getChunkY();
            var z = section.getChunkZ();

            result.patchMarkPresent(x, y, z);
        }
    }
}
