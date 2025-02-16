package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;

public interface RenderListProvider extends SortItemsProvider {
    ObjectArrayList<ChunkRenderList> getUnsortedRenderLists();

    boolean orderIsUnsorted();

    default SortedRenderLists createRenderLists(Viewport viewport) {
        var doSorting = this.orderIsUnsorted();

        var sectionPos = viewport.getChunkCoord();
        var renderLists = this.getUnsortedRenderLists();

        // sort the regions by distance to fix rare region ordering bugs if necessary
        if (doSorting) {
            var cameraX = sectionPos.getX() >> RenderRegion.REGION_WIDTH_SH;
            var cameraY = sectionPos.getY() >> RenderRegion.REGION_HEIGHT_SH;
            var cameraZ = sectionPos.getZ() >> RenderRegion.REGION_LENGTH_SH;

            var size = renderLists.size();
            var sortItems = this.ensureSortItemsOfLength(size);

            for (var i = 0; i < size; i++) {
                var region = renderLists.get(i).getRegion();
                var x = Math.abs(region.getX() - cameraX);
                var y = Math.abs(region.getY() - cameraY);
                var z = Math.abs(region.getZ() - cameraZ);
                sortItems[i] = (x + y + z) << 16 | i;
            }

            IntArrays.unstableSort(sortItems, 0, size);

            var sorted = new ObjectArrayList<ChunkRenderList>(size);
            for (var i = 0; i < size; i++) {
                var key = sortItems[i];
                var renderList = renderLists.get(key & 0xFFFF);
                sorted.add(renderList);
            }
            renderLists = sorted;
        }

        for (var list : renderLists) {
            list.prepareForRender(sectionPos, this, doSorting);
        }

        return new SortedRenderLists(renderLists);
    }
}
