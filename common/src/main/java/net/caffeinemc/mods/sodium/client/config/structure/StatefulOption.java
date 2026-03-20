package net.caffeinemc.mods.sodium.client.config.structure;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class StatefulOption<V> extends Option {
    final StorageEventHandler storage;
    final Function<V, Component> tooltipProvider;
    final OptionImpact impact;
    final Set<Identifier> flags;
    final DependentValue<V> defaultValue;
    final Boolean controlHiddenWhenDisabled;
    final OptionBinding<V> binding;
    final Consumer<ConfigState> applyHook;
    final Identifier applyHookId;

    private final Collection<DynamicValue<?>> dependents = new ObjectOpenHashSet<>(0);
    private final Collection<DynamicValue<?>> applyDependents = new ObjectOpenHashSet<>(0);

    private V value;
    private V modifiedValue;

    StatefulOption(
            Identifier id,
            Collection<Identifier> dependencies,
            Component name,
            DependentValue<Boolean> enabled,
            StorageEventHandler storage,
            Function<V, Component> tooltipProvider,
            OptionImpact impact,
            Set<Identifier> flags,
            DependentValue<V> defaultValue,
            Boolean controlHiddenWhenDisabled,
            OptionBinding<V> binding,
            Consumer<ConfigState> applyHook
    ) {
        super(id, dependencies, name, enabled);
        this.storage = storage;
        this.tooltipProvider = tooltipProvider;
        this.impact = impact;
        this.flags = flags;
        this.defaultValue = defaultValue;
        this.controlHiddenWhenDisabled = controlHiddenWhenDisabled;
        this.binding = binding;
        this.applyHook = applyHook;

        if (applyHook != null) {
            this.applyHookId = Identifier.fromNamespaceAndPath("__meta__", "apply_hook_" + id.getNamespace() + "_" + id.getPath());
        } else {
            this.applyHookId = null;
        }
    }

    @Override
    void visitDependentValues(Consumer<DependentValue<?>> visitor) {
        super.visitDependentValues(visitor);
        visitor.accept(this.defaultValue);
    }

    void registerDependent(DynamicValue<?> dependent) {
        this.dependents.add(dependent);
    }

    void registerApplyDependent(DynamicValue<?> dependent) {
        this.applyDependents.add(dependent);
    }

    public void modifyValue(V value) {
        if (this.modifiedValue != value) {
            this.modifiedValue = value;
            this.state.invalidateDependents(this.dependents);
        }
    }

    @Override
    void loadValueInitial() {
        this.value = this.binding.load();
        this.modifiedValue = this.value;
    }

    @Override
    void resetFromBinding() {
        var previousValue = this.modifiedValue;
        this.value = this.binding.load();

        var newValue = this.validateValue(this.value);
        if (newValue != this.value) {
            this.value = newValue;
            this.binding.save(this.value);
            this.state.notifyStorageWrite(this.storage);
        }

        this.modifiedValue = this.value;
        if (this.value != previousValue) {
            this.state.invalidateDependents(this.dependents);
            this.state.invalidateDependents(this.applyDependents);
        }
    }

    public V getValidatedValue() {
        var newValue = this.validateValue(this.modifiedValue);
        if (newValue != this.modifiedValue) {
            this.modifiedValue = newValue;
            this.state.invalidateDependents(this.dependents);
        }

        return this.modifiedValue;
    }

    public V getAppliedValue() {
        return this.value;
    }

    abstract V validateValue(V value);

    @Override
    public boolean hasChanged() {
        return this.modifiedValue != this.value;
    }

    @Override
    boolean applyChanges() {
        if (this.hasChanged()) {
            this.value = this.modifiedValue;
            this.binding.save(this.value);
            this.state.notifyStorageWrite(this.storage);
            this.state.invalidateDependents(this.applyDependents);
            return true;
        }
        return false;
    }

    @Override
    public OptionImpact getImpact() {
        return this.impact;
    }

    @Override
    public Component getTooltip() {
        return this.tooltipProvider.apply(this.getValidatedValue());
    }

    @Override
    public Set<Identifier> getFlags() {
        return this.flags;
    }

    public StorageEventHandler getStorage() {
        return this.storage;
    }

    public Function<V, Component> getTooltipProvider() {
        return this.tooltipProvider;
    }

    public DependentValue<V> getDefaultValue() {
        return this.defaultValue;
    }

    public Boolean getControlHiddenWhenDisabled() {
        return this.controlHiddenWhenDisabled;
    }

    public boolean showControl() {
        if (this.isEnabled()) {
            return true;
        }
        if (this.controlHiddenWhenDisabled == null) {
            return false;
        }
        return !this.controlHiddenWhenDisabled;
    }

    public OptionBinding<V> getBinding() {
        return this.binding;
    }

    public Consumer<ConfigState> getApplyHook() {
        return this.applyHook;
    }

    public Identifier getApplyHookId() {
        return this.applyHookId;
    }
}
