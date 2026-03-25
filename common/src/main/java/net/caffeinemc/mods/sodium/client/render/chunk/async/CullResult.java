package net.caffeinemc.mods.sodium.client.render.chunk.async;

import net.caffeinemc.mods.sodium.client.render.chunk.lists.DeferredTaskList;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;

public interface CullResult {
    SectionTree getCullTreeWide();

    SectionTree getCullTreeRegular();

    SectionTree getCullTreeLocal();

    DeferredTaskList getPendingTaskLists();
}
