package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.storage.SectionStorage;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.world.level.Level;

public class FallbackVisibleChunkCollector extends TaskCollectingTree implements CoordinateSectionVisitor {
    private final SectionStorage sections;
    private final VisibleChunkCollector renderListCollector;

    public FallbackVisibleChunkCollector(Viewport viewport, float buildDistance, int frame, SectionStorage sections, RenderRegionManager regions, Level level) {
        super(viewport, buildDistance, frame, CullType.LOCAL, level);
        this.sections = sections;
        this.renderListCollector = new VisibleChunkCollector(regions, frame);
    }

    public SortedRenderLists createRenderLists(Viewport viewport) {
        return this.renderListCollector.createRenderLists(viewport);
    }

    @Override
    public void visit(int x, int y, int z) {
        this.renderListCollector.visit(x, y, z);

        var section = this.sections.getCurrent(x, y, z);

        if (section == null) {
            return;
        }

        this.visit(section, true);
    }
}
