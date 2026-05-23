package net.caffeinemc.mods.sodium.client.util;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

public class IgnoringViewArea extends ViewArea {
    public IgnoringViewArea(SectionRenderDispatcher sectionRenderDispatcher) {
        super(sectionRenderDispatcher, 0, 0, 0, 0, 0, null);
    }

    @Override
    public void releaseAllBuffers() {

    }

    @Override
    public boolean repositionCamera(SectionPos cameraSectionPos) {
        return false;
    }

    @Override
    public SectionRenderDispatcher.@Nullable RenderSection getRenderSectionAt(BlockPos pos) {
        return null;
    }

    @Override
    protected SectionRenderDispatcher.@Nullable RenderSection getRenderSection(long sectionNode) {
        return null;
    }
}
