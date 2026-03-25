package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import net.minecraft.core.SectionPos;

public class DeferredTaskList extends LongHeapPriorityQueue {
    private final int baseOffsetX;
    private final int baseOffsetZ;

    public static DeferredTaskList createHeapCopyOf(LongCollection copyFrom, int baseOffsetX, int baseOffsetZ) {
        return new DeferredTaskList(new LongArrayList(copyFrom), baseOffsetX, baseOffsetZ);
    }

    private DeferredTaskList(LongArrayList copyFrom, int baseOffsetX, int baseOffsetZ) {
        super(copyFrom.elements(), copyFrom.size());
        this.baseOffsetX = baseOffsetX;
        this.baseOffsetZ = baseOffsetZ;
    }

    public long dequeueNextSectionPos() {
        var encoded = this.dequeueLong();

        var localX = (int) (encoded >>> 20) & 0b1111111111;
        var localY = (int) (encoded >>> 10) & 0b1111111111;
        var localZ = (int) (encoded & 0b1111111111);

        var globalX = localX + this.baseOffsetX;
        var globalY = localY + TaskCollectingTree.SECTION_Y_MIN;
        var globalZ = localZ + this.baseOffsetZ;

        return SectionPos.asLong(globalX, globalY, globalZ);
    }
}
