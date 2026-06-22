package net.caffeinemc.mods.sodium.client.render.chunk.storage;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.core.SectionPos;

/**
 * Controls access to the map of render sections.
 *
 * During the safe read phase it is safe to call {@code getCurrent} methods on a thread other than the one that started the phase. Otherwise, only one thread may use this structure at a time, and starting and ending the safe phase is also not allowed concurrently with other operations.
 */
public interface SectionStorage {
    default RenderSection getCurrent(SectionPos pos) {
        return this.getCurrent(pos.asLong());
    }

    default RenderSection getCurrent(int x, int y, int z) {
        return this.getCurrent(SectionPos.asLong(x, y, z));
    }

    RenderSection getCurrent(long key);

    RenderSection getConsistent(long key);

    boolean hasSectionConsistent(long key);

    void queuePut(long key, RenderSection newSection);

    RenderSection queueRemove(long key);

    void deleteAll();

    int size();

    void startSafeReadPhase();

    void endSafeReadPhase();

    String getDebugInfo();
}
