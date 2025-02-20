package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;

/**
 * The occlusion section collector is passed to the occlusion graph search culler to
 * collect the visible chunks.
 */
public class OcclusionSectionCollector extends SectionCollector {
    public OcclusionSectionCollector(int frame, TaskQueueType importantRebuildQueueType) {
        super(frame, importantRebuildQueueType);
    }

    @Override
    public boolean orderIsSorted() {
        return false;
    }
}
