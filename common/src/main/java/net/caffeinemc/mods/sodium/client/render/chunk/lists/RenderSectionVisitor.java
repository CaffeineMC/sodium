package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;

public interface RenderSectionVisitor {
    void visit(RenderSection section);
}
