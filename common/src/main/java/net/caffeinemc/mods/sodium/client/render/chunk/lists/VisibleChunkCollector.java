package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateTypes;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;

/**
 * The visible chunk collector is passed to the occlusion graph search culler to collect the visible chunks.
 */
public class VisibleChunkCollector implements OcclusionCuller.Visitor {
    private final ObjectArrayList<ChunkRenderList> sortedRenderLists;
    private final EnumMap<TaskQueueType, ArrayDeque<RenderSection>> sortedRebuildLists;

    private final int frame;
    private final TaskQueueType importantRebuildQueueType;

    public VisibleChunkCollector(int frame, TaskQueueType importantRebuildQueueType) {
        this.frame = frame;
        this.importantRebuildQueueType = importantRebuildQueueType;

        this.sortedRenderLists = new ObjectArrayList<>();
        this.sortedRebuildLists = new EnumMap<>(TaskQueueType.class);

        for (var type : TaskQueueType.values()) {
            this.sortedRebuildLists.put(type, new ArrayDeque<>());
        }
    }

    @Override
    public void visit(RenderSection section) {
        // only process section (and associated render list) if it has content that needs rendering
        if (section.getFlags() != 0) {
            RenderRegion region = section.getRegion();
            ChunkRenderList renderList = region.getRenderList();

            if (renderList.getLastVisibleFrame() != this.frame) {
                renderList.reset(this.frame);

                this.sortedRenderLists.add(renderList);
            }

            renderList.add(section);
        }

        // always add to rebuild lists though, because it might just not be built yet
        this.addToRebuildLists(section);
    }

    private void addToRebuildLists(RenderSection section) {
        var pendingUpdate = section.getPendingUpdate();

        if (pendingUpdate != 0 && section.getTaskCancellationToken() == null) {
            var queueType = ChunkUpdateTypes.getQueueType(pendingUpdate, this.importantRebuildQueueType);
            Queue<RenderSection> queue = this.sortedRebuildLists.get(queueType);

            if (queue.size() < queueType.queueSizeLimit()) {
                queue.add(section);
            }
        }
    }

    private static int[] sortItems = new int[RenderRegion.REGION_SIZE];

    public SortedRenderLists createRenderLists(Viewport viewport) {
        // sort the regions by distance to fix rare region ordering bugs
        var sectionPos = viewport.getChunkCoord();
        var cameraX = sectionPos.getX() >> RenderRegion.REGION_WIDTH_SH;
        var cameraY = sectionPos.getY() >> RenderRegion.REGION_HEIGHT_SH;
        var cameraZ = sectionPos.getZ() >> RenderRegion.REGION_LENGTH_SH;
        var size = this.sortedRenderLists.size();

        if (sortItems.length < size) {
            sortItems = new int[size];
        }

        for (var i = 0; i < size; i++) {
            var region = this.sortedRenderLists.get(i).getRegion();
            var x = Math.abs(region.getX() - cameraX);
            var y = Math.abs(region.getY() - cameraY);
            var z = Math.abs(region.getZ() - cameraZ);
            sortItems[i] = (x + y + z) << 16 | i;
        }

        IntArrays.unstableSort(sortItems, 0, size);

        var sorted = new ObjectArrayList<ChunkRenderList>(size);
        for (var i = 0; i < size; i++) {
            var key = sortItems[i];
            var renderList = this.sortedRenderLists.get(key & 0xFFFF);
            sorted.add(renderList);
        }

        // sort sections and invalidate batch caches if the render lists changed
        for (var list : sorted) {
            list.prepareForRender(sectionPos, sortItems);
        }

        return new SortedRenderLists(sorted);
    }

    public Map<TaskQueueType, ArrayDeque<RenderSection>> getRebuildLists() {
        return this.sortedRebuildLists;
    }
}
