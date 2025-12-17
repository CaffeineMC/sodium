package net.caffeinemc.mods.sodium.client.config.value;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public class DynamicValue<V> implements DependentValue<V>, ConfigState {
    private final Set<ResourceLocation> dependencies;
    private ResourceLocation parentOption = null;
    private final Function<ConfigState, V> provider;
    private Config state;
    private V valueCache;

    public DynamicValue(Function<ConfigState, V> provider, ResourceLocation[] dependencies) {
        this.provider = provider;
        this.dependencies = Set.of(dependencies);
    }

    @Override
    public V get(Config state) {
        if (this.valueCache != null) {
            return this.valueCache;
        }

        this.state = state;
        this.valueCache = this.provider.apply(this);
        this.state = null;
        return this.valueCache;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return this.dependencies;
    }

    public void allowReadingParentOption(ResourceLocation id) {
        this.parentOption = id;
    }

    public void invalidateCache() {
        this.valueCache = null;
    }

    private boolean getReadType(ResourceLocation id) {
        if (!this.dependencies.contains(id)) {
            if (id.equals(this.parentOption)) {
                return true;
            }
            throw new IllegalStateException("Attempted to read option value that is not a declared dependency");
        }
        return false;
    }

    // TODO: resolve dependencies with update tag here or within ConfigStateImpl?
    @Override
    public boolean readBooleanOption(ResourceLocation id) {
        var readType = this.getReadType(id);
        return this.state.readBooleanOption(id, readType);
    }

    @Override
    public int readIntOption(ResourceLocation id) {
        var readType = this.getReadType(id);
        return this.state.readIntOption(id, readType);
    }

    @Override
    public <E extends Enum<E>> E readEnumOption(ResourceLocation id, Class<E> enumClass) {
        var readType = this.getReadType(id);
        return this.state.readEnumOption(id, enumClass, readType);
    }
}
