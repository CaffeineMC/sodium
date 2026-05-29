package net.caffeinemc.mods.sodium.client.gl.device;

public interface DrawCommandList extends AutoCloseable {
    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
