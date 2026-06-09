package net.caffeinemc.mods.sodium.client.render.chunk.map;

import net.caffeinemc.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.network.chat.Component;

public enum LoadedChunkEdgeRendering implements TextProvider {
    OFF("sodium.options.render_loaded_chunk_edges.off"),
    PERFORMANCE("sodium.options.render_loaded_chunk_edges.performance"),
    IMMEDIATE("sodium.options.render_loaded_chunk_edges.immediate");

    private final Component name;

    LoadedChunkEdgeRendering(String name) {
        this.name = Component.translatable(name);
    }

    @Override
    public Component getLocalizedName() {
        return this.name;
    }
}
