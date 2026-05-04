package net.caffeinemc.mods.sodium.client.render.chunk.async;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.DeferredTaskList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.TaskCollectingTree;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.RayOcclusionSectionTree;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.LinkedList;

public class CullTask extends AsyncRenderTask<CullResult> {
    protected final OcclusionCuller occlusionCuller;
    protected final boolean useOcclusionCulling;
    private final float searchDistanceRegular;
    private final float searchDistanceLocal;
    private LinkedList<Collection<RenderSection>> presentPatches;
    private final Level level;

    public CullTask(Viewport viewport, float searchDistanceRegular, float searchDistanceLocal, int frame, OcclusionCuller occlusionCuller, boolean useOcclusionCulling, Level level) {
        super(viewport, frame);
        this.searchDistanceRegular = searchDistanceRegular;
        this.searchDistanceLocal = searchDistanceLocal;
        this.occlusionCuller = occlusionCuller;
        this.useOcclusionCulling = useOcclusionCulling;
        this.level = level;
    }

    @Override
    protected CullResult runTask() {
        var wideTree = new TaskCollectingTree(this.viewport, this.searchDistanceRegular, this.frame, CullType.WIDE, this.level);
        var regularTree = new SectionTree(this.viewport, this.searchDistanceRegular, this.frame, CullType.REGULAR, this.level);
        var localTree = new RayOcclusionSectionTree(this.viewport, this.searchDistanceLocal, this.frame, CullType.LOCAL, this.level);

        this.occlusionCuller.findVisible(wideTree, regularTree, localTree, this.viewport, this.searchDistanceRegular, this.searchDistanceLocal, this.useOcclusionCulling, this);

        wideTree.prepareForTraversal();
        regularTree.prepareForTraversal();
        localTree.prepareForTraversal();

        var taskLists = wideTree.getPendingTaskLists();

        return new CullResult() {
            @Override
            public SectionTree getCullTreeWide() {
                CullTask.this.applyPresentPatches(wideTree);
                return wideTree;
            }

            @Override
            public SectionTree getCullTreeRegular() {
                CullTask.this.applyPresentPatches(regularTree);
                return regularTree;
            }

            @Override
            public SectionTree getCullTreeLocal() {
                CullTask.this.applyPresentPatches(localTree);
                return localTree;
            }

            @Override
            public DeferredTaskList getPendingTaskLists() {
                return taskLists;
            }
        };
    }

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
