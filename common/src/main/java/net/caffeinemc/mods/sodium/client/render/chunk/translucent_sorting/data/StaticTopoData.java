package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.minecraft.core.SectionPos;

import java.nio.IntBuffer;
import java.util.function.IntConsumer;

/**
 * Static topo acyclic sorting uses the topo sorting algorithm but only if it's
 * possible to sort without dynamic triggering, meaning the sort order never
 * needs to change.
 */
public class StaticTopoData extends PresentTranslucentData {
    private Sorter sorterOnce;

    StaticTopoData(SectionPos sectionPos, int inputQuadCount) {
        super(sectionPos, inputQuadCount);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO;
    }

    @Override
    public Sorter getSorter() {
        var sorter = this.sorterOnce;
        if (sorter == null) {
            throw new IllegalStateException("Sorter already used!");
        }
        this.sorterOnce = null;
        return sorter;
    }

    private record QuadIndexConsumerIntoBuffer(IntBuffer buffer) implements IntConsumer {
        // Track if we have already written at least one quad into this buffer
        private static final ThreadLocal<boolean[]> STATE = ThreadLocal.withInitial(() -> new boolean[] { false });

        @Override
        public void accept(int value) {
            var state = STATE.get();
            if (state[0]) {
                // Insert primitive-restart between successive quads
                this.buffer.put(TranslucentData.RESTART);
            }
            TranslucentData.writeQuadVertexIndexes(this.buffer, value, false);
            state[0] = true;
        }
    }

    public static StaticTopoData fromMesh(TQuad[] quads, SectionPos sectionPos, boolean failOnIntersection) {
        var sorter = new StaticSorter(quads.length);
        var indexWriter = new QuadIndexConsumerIntoBuffer(sorter.getIntBuffer());

        // Reset "first quad" state before sorting/writing starts
        QuadIndexConsumerIntoBuffer.STATE.get()[0] = false;

        if (!TopoGraphSorting.topoGraphSort(indexWriter, quads, null, null, failOnIntersection)) {
            sorter.getIndexBuffer().free();
            return null;
        }

        var staticTopoData = new StaticTopoData(sectionPos, quads.length);
        staticTopoData.sorterOnce = sorter;
        return staticTopoData;
    }
}
