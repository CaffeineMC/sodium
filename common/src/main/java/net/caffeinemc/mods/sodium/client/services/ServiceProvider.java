package net.caffeinemc.mods.sodium.client.services;

public interface ServiceProvider<T> {
    boolean isEnabled();

    T get();
}
