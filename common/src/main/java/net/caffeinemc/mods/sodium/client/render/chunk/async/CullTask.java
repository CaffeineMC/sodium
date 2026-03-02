package net.caffeinemc.mods.sodium.client.render.chunk.async;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;

import java.util.Collection;
import java.util.LinkedList;

public abstract class CullTask<T> extends AsyncRenderTask<T> {
    protected final OcclusionCuller occlusionCuller;
    protected final boolean useOcclusionCulling;
    private LinkedList<Collection<RenderSection>> presentPatches;

    protected CullTask(Viewport viewport, float buildDistance, int frame, OcclusionCuller occlusionCuller, boolean useOcclusionCulling) {
        super(viewport, buildDistance, frame);
        this.occlusionCuller = occlusionCuller;
        this.useOcclusionCulling = useOcclusionCulling;
    }

    public abstract CullType getCullType();

    @Override
    public void registerPresentPatches(Collection<RenderSection> presentPatches) {
        // maintain a list of present patch sets because the task may receive multiple patch sets if it runs for longer than a frame and multiple instances of patching are required. We don't want to simply .addAll the collection because multiple tasks may be sharing the same patch set.
        if (this.presentPatches == null) {
            this.presentPatches = new LinkedList<>();
        }
        this.presentPatches.add(presentPatches);
    }

    protected void applyPresentPatches(SectionTree result) {
        if (this.presentPatches == null) {
            return;
        }

        for (var patchList : this.presentPatches) {
            for (var section : patchList) {
                var x = section.getChunkX();
                var y = section.getChunkY();
                var z = section.getChunkZ();

                result.patchMarkPresent(x, y, z);
            }
        }
    }
}
