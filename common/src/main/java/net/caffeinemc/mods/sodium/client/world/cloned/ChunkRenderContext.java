package net.caffeinemc.mods.sodium.client.world.cloned;

import net.minecraft.core.SectionPos;

import java.util.List;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final List<?> renderers;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, List<?> renderers) {
        this.origin = origin;
        this.sections = sections;
        this.renderers = renderers;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    public List<?> getRenderers() {
        return this.renderers;
    }
}
