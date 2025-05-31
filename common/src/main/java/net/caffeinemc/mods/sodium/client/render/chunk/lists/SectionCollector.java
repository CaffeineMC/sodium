package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateTypes;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;

public abstract class SectionCollector implements RenderListProvider, RenderSectionVisitor {
    private final int frame;
    private final TaskQueueType importantRebuildQueueType;
    private final ObjectArrayList<ChunkRenderList> renderLists;
    private final EnumMap<TaskQueueType, ArrayDeque<RenderSection>> sortedTaskLists;

    private static int[] sortItems = new int[RenderRegion.REGION_SIZE];

    public SectionCollector(int frame, TaskQueueType importantRebuildQueueType) {
        this.frame = frame;
        this.importantRebuildQueueType = importantRebuildQueueType;

        this.renderLists = new ObjectArrayList<>();
        this.sortedTaskLists = new EnumMap<>(TaskQueueType.class);

        for (var type : TaskQueueType.values()) {
            this.sortedTaskLists.put(type, new ArrayDeque<>());
        }
    }

    @Override
    public void visit(RenderSection section) {
        // only process section (and associated render list) if it has content that needs rendering
        if (section.getFlags() != 0) {
            RenderRegion region = section.getRegion();
            ChunkRenderList renderList = region.getRenderList();

            if (renderList.getLastVisibleFrame() != this.frame) {
                renderList.reset(this.frame, this.orderIsSorted());

                this.renderLists.add(renderList);
            }

            renderList.add(section);
        }

        // always add to rebuild lists though, because it might just not be built yet
        var pendingUpdate = section.getPendingUpdate();

        if (pendingUpdate != 0) {
            var queueType = ChunkUpdateTypes.getQueueType(pendingUpdate, this.importantRebuildQueueType);
            Queue<RenderSection> queue = this.sortedTaskLists.get(queueType);

            if (queue.size() < queueType.queueSizeLimit()) {
                queue.add(section);
            }
        }
    }

    @Override
    public ObjectArrayList<ChunkRenderList> getUnsortedRenderLists() {
        return this.renderLists;
    }

    @Override
    public Map<TaskQueueType, ArrayDeque<RenderSection>> getTaskLists() {
        return this.sortedTaskLists;
    }

    @Override
    public void setCachedSortItems(int[] sortItems) {
        SectionCollector.sortItems = sortItems;
    }

    @Override
    public int[] getCachedSortItems() {
        return SectionCollector.sortItems;
    }
}
