package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.minecraft.core.SectionPos;

/**
 * Collects sections from a tree traversal. It needs to turn coordinates into section objects because the section collector is not capable of handling raw section indexes yet.
 */
public class TreeSectionCollector extends SectionCollector implements CoordinateSectionVisitor {
    private final Long2ReferenceMap<RenderSection> sections;

    public TreeSectionCollector(int frame, TaskQueueType importantRebuildQueueType, Long2ReferenceMap<RenderSection> sections) {
        super(frame, importantRebuildQueueType);
        this.sections = sections;
    }

    @Override
    public void visit(int x, int y, int z) {
        var section = this.sections.get(SectionPos.asLong(x, y, z));

        if (section != null) {
            this.visit(section);
        }
    }

    @Override
    public boolean orderIsSorted() {
        return true;
    }
}
