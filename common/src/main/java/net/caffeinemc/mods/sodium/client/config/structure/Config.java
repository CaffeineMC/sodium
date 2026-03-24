package net.caffeinemc.mods.sodium.client.config.structure;

import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.client.config.search.BigramSearchIndex;
import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.caffeinemc.mods.sodium.client.config.search.SearchQuerySession;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

public class Config implements ConfigState {
    private final Map<Identifier, Option> options = new Object2ReferenceLinkedOpenHashMap<>();
    private final Set<StorageEventHandler> pendingStorageHandlers = new ObjectOpenHashSet<>();
    private final List<ModOptions> modOptions;
    private final SearchIndex searchIndex = new BigramSearchIndex(this::registerSearchIndex);
    private final Collection<DynamicValue<?>> globalRebuildDependents = new ObjectArrayList<>();
    private final Map<Identifier, Collection<FlagHook>> flagHooks = new Object2ReferenceOpenHashMap<>();
    private final Set<FlagHook> triggeredHooks = new ObjectOpenHashSet<>();

    public Config(List<ModOptions> modOptions) {
        this.modOptions = Collections.unmodifiableList(modOptions);

        this.collectOptions();
        this.applyOptionChanges();
        this.collectApplyHooks();
        this.validateDependencies();

        // load options initially from their bindings
        for (var option : this.options.values()) {
            option.loadValueInitial();
        }
        resetAllOptionsFromBindings();
    }

    private void registerSearchIndex() {
        for (var modConfig : this.modOptions) {
            modConfig.registerTextSources(this.searchIndex);
        }
    }

    public SearchQuerySession startSearchQuery() {
        return this.searchIndex.startQuery();
    }

    private void registerHook(FlagHook hook) {
        for (var trigger : hook.getTriggers()) {
            this.flagHooks.computeIfAbsent(trigger, k -> new ObjectArrayList<>()).add(hook);
        }
    }

    private void collectOptions() {
        for (var modConfig : this.modOptions) {
            for (var page : modConfig.pages()) {
                for (var group : page.groups()) {
                    for (var option : group.options()) {
                        this.options.put(option.id, option);
                        option.setParentConfig(this);
                    }
                }
            }

            if (modConfig.flagHooks() != null) {
                for (var hook : modConfig.flagHooks()) {
                    this.registerHook(hook);
                }
            }
        }
    }

    private void applyOptionChanges() {
        var overrides = new Object2ReferenceOpenHashMap<Identifier, OptionOverride>();
        var overlays = new Object2ReferenceOpenHashMap<Identifier, OptionOverlay>();

        // collect overrides and overlays and validate them, also against each other
        for (var modConfig : this.modOptions) {
            for (var override : modConfig.overrides()) {
                if (override.target().getNamespace().equals(modConfig.configId())) {
                    throw new IllegalArgumentException("Override by mod '" + modConfig.configId() + "' targets its own option '" + override.target() + "'");
                }

                if (overrides.put(override.target(), override) != null) {
                    throw new IllegalArgumentException("Multiple overrides for option '" + override.target() + "'");
                }
            }

            for (var overlay : modConfig.overlays()) {
                if (overlay.target().getNamespace().equals(modConfig.configId())) {
                    throw new IllegalArgumentException("Overlay by mod '" + modConfig.configId() + "' targets its own option '" + overlay.target() + "'");
                }

                if (overlays.put(overlay.target(), overlay) != null) {
                    throw new IllegalArgumentException("Multiple overlays for option '" + overlay.target() + "'");
                }
            }
        }

        // apply overrides
        for (var modConfig : this.modOptions) {
            for (var page : modConfig.pages()) {
                for (var group : page.groups()) {
                    var options = group.options();
                    for (int i = 0; i < options.size(); i++) {
                        var option = options.get(i);
                        var override = overrides.get(option.id);

                        // apply override to option if it exists
                        if (override != null) {
                            var replacement = override.change();
                            exchangeOption(options, i, replacement, option);
                        }
                    }
                }
            }
        }

        // apply overlays
        for (var modConfig : this.modOptions) {
            for (var page : modConfig.pages()) {
                for (var group : page.groups()) {
                    var options = group.options();
                    for (int i = 0; i < options.size(); i++) {
                        var option = options.get(i);
                        var overlay = overlays.get(option.id);

                        // apply overlay to option if it exists
                        if (overlay != null) {
                            var change = overlay.change();
                            try {
                                var overlaidOption = change.buildWithBaseOption(option);
                                exchangeOption(options, i, overlaidOption, option);
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Failed to apply overlay from '" + overlay.source() + "' to option '" + option.id + "'", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void exchangeOption(List<Option> optionGroupList, int i, Option replacement, Option original) {
        optionGroupList.set(i, replacement);
        this.options.remove(original.id);
        this.options.put(replacement.id, replacement);
        replacement.setParentConfig(this);
        original.setParentConfig(null);
    }

    private static final Set<Identifier> SPECIAL_DEPENDENCIES = Set.of(
            ConfigState.UPDATE_ON_REBUILD,
            ConfigState.UPDATE_ON_APPLY
    );

    private record ApplyHookFlagHook(Identifier applyHookId, Consumer<ConfigState> applyHook) implements FlagHook {
        @Override
        public Collection<Identifier> getTriggers() {
            return Set.of(this.applyHookId);
        }

        @Override
        public void accept(Collection<Identifier> identifiers, ConfigState configState) {
            this.applyHook.accept(configState);
        }
    }

    private void collectApplyHooks() {
        // collect all apply hooks and convert them into singleton flag hooks
        for (var option : this.options.values()) {
            if (option instanceof StatefulOption<?> statefulOption) {
                var applyHook = statefulOption.getApplyHook();
                if (applyHook == null) {
                    continue;
                }
                this.registerHook(new ApplyHookFlagHook(statefulOption.getApplyHookId(), applyHook));
            }
        }
    }

    private void validateDependencies() {
        for (var option : this.options.values()) {
            for (var dependency : option.dependencies) {
                if (!this.options.containsKey(dependency) && !SPECIAL_DEPENDENCIES.contains(dependency)) {
                    throw new IllegalArgumentException("Option " + option.id + " depends on non-existent option " + dependency);
                }
            }

            // link dependents
            option.visitDependentValues(dependent -> {
                if (dependent instanceof DynamicValue<?> dynamicValue) {
                    for (var dependency : dependent.getDependencies()) {
                        if (dependency.equals(ConfigState.UPDATE_ON_REBUILD)) {
                            this.globalRebuildDependents.add(dynamicValue);
                            continue;
                        }

                        if (dependency.equals(ConfigState.UPDATE_ON_APPLY) && option instanceof StatefulOption<?> statefulOption) {
                            statefulOption.registerApplyDependent(dynamicValue);
                            dynamicValue.allowReadingParentOption(option.id);
                            continue;
                        }

                        var dependencyOption = this.options.get(dependency);
                        if (dependencyOption instanceof StatefulOption<?> statefulOption) {
                            statefulOption.registerDependent(dynamicValue);
                        }
                    }
                }
            });
        }

        // make sure there are no cycles
        var stack = new ObjectOpenHashSet<Identifier>();
        var finished = new ObjectOpenHashSet<Identifier>();
        for (var option : this.options.values()) {
            this.checkDependencyCycles(option, stack, finished);
        }
    }

    void invalidateDependents(Collection<DynamicValue<?>> dependents) {
        for (var dependent : dependents) {
            dependent.invalidateCache();
        }
    }

    private void checkDependencyCycles(Option option, ObjectOpenHashSet<Identifier> stack, ObjectOpenHashSet<Identifier> finished) {
        if (!stack.add(option.id)) {
            throw new IllegalArgumentException("Cycle detected in dependency graph starting from option " + option.id);
        }

        for (var dependency : option.dependencies) {
            if (finished.contains(dependency)) {
                continue;
            }
            Option dependencyOption = this.options.get(dependency);
            if (dependencyOption != null) {
                this.checkDependencyCycles(dependencyOption, stack, finished);
            }
        }

        stack.remove(option.id);
        finished.add(option.id);
    }

    public void resetAllOptionsFromBindings() {
        for (var option : this.options.values()) {
            option.resetFromBinding();
        }
    }

    public void applyAllOptions() {
        Set<Identifier> flags = null;

        for (var option : this.options.values()) {
            if (option.applyChanges()) {
                var optionFlags = option.getFlags();
                if (optionFlags != null && !optionFlags.isEmpty()) {
                    if (flags == null) {
                        flags = new ObjectOpenHashSet<>();
                    }
                    flags.addAll(optionFlags);
                }

                if (option instanceof StatefulOption<?> statefulOption) {
                    var applyHookId = statefulOption.getApplyHookId();
                    if (applyHookId != null) {
                        if (flags == null) {
                            flags = new ObjectOpenHashSet<>();
                        }
                        flags.add(applyHookId);
                    }
                }
            }
        }

        this.flushStorageHandlers();

        if (flags == null) {
            return;
        }
        processFlags(flags);
    }

    public void applyOption(Identifier id) {
        Set<Identifier> flags = null;

        var option = this.options.get(id);
        if (option != null && option.applyChanges()) {
            flags = option.getFlags();
        }

        this.flushStorageHandlers();

        if (flags == null) {
            return;
        }
        processFlags(flags);
    }

    public boolean anyOptionChanged() {
        for (var option : this.options.values()) {
            if (option.hasChanged()) {
                return true;
            }
        }

        return false;
    }

    public void invalidateGlobalRebuildDependents() {
        this.invalidateDependents(this.globalRebuildDependents);
    }

    void notifyStorageWrite(StorageEventHandler handler) {
        this.pendingStorageHandlers.add(handler);
    }

    void flushStorageHandlers() {
        for (var handler : this.pendingStorageHandlers) {
            handler.afterSave();
        }
        this.pendingStorageHandlers.clear();
    }

    public Option getOption(Identifier id) {
        return this.options.get(id);
    }

    public List<ModOptions> getModOptions() {
        return this.modOptions;
    }

    private void processFlags(Set<Identifier> flags) {
        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD.getId())) {
            onRendererReload();
        } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE.getId())) {
            onRendererUpdate();
        }

        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD.getId())) {
            onAssetReload();
        }

        if (flags.contains(OptionFlag.REQUIRES_VIDEOMODE_RELOAD.getId())) {
            onVideoModeReload();
        }

        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART.getId())) {
            onGameNeedsRestart();
        }

        // process the registered flag hooks
        this.triggeredHooks.clear();
        var immutableFlags = Collections.unmodifiableSet(flags);
        for (var flag : flags) {
            var hooks = this.flagHooks.get(flag);
            if (hooks != null) {
                for (var hook : hooks) {
                    if (this.triggeredHooks.add(hook)) {
                        hook.accept(immutableFlags, this);
                    }
                }
            }
        }
    }

    public static void onRendererUpdate() {
        var client = Minecraft.getInstance();
        if (client.level != null) {
            client.levelRenderer.needsUpdate();
        }
    }

    public static void onRendererReload() {
        var client = Minecraft.getInstance();
        if (client.level != null) {
            client.levelRenderer.allChanged();
        }
    }

    public static void onAssetReload() {
        var client = Minecraft.getInstance();
        client.updateMaxMipLevel(client.options.mipmapLevels().get());
        client.delayTextureReload();
    }

    public static void onVideoModeReload() {
        var client = Minecraft.getInstance();
        client.getWindow().changeFullscreenVideoMode();
    }

    public static void onGameNeedsRestart() {
        Console.instance().logMessage(MessageLevel.WARN,
                "sodium.console.game_restart", true, 10.0);
    }

    public boolean readBooleanOption(Identifier id, boolean appliedValue) {
        var option = this.options.get(id);
        if (option instanceof BooleanOption booleanOption) {
            if (appliedValue) {
                return booleanOption.getAppliedValue();
            } else {
                return booleanOption.getValidatedValue();
            }
        }

        throw new IllegalArgumentException("Can't read boolean value from option with id " + id);
    }

    public int readIntOption(Identifier id, boolean appliedValue) {
        var option = this.options.get(id);
        if (option instanceof IntegerOption intOption) {
            if (appliedValue) {
                return intOption.getAppliedValue();
            } else {
                return intOption.getValidatedValue();
            }
        }

        throw new IllegalArgumentException("Can't read int value from option with id " + id);
    }

    public <E extends Enum<E>> E readEnumOption(Identifier id, Class<E> enumClass, boolean appliedValue) {
        var option = this.options.get(id);
        if (option instanceof EnumOption<?> enumOption) {
            if (enumOption.enumClass != enumClass) {
                throw new IllegalArgumentException("Enum class mismatch for option with id " + id + ": requested " + enumClass + ", option has " + enumOption.enumClass);
            }

            if (appliedValue) {
                return enumClass.cast(enumOption.getAppliedValue());
            } else {
                return enumClass.cast(enumOption.getValidatedValue());
            }
        }

        throw new IllegalArgumentException("Can't read enum value from option with id " + id);
    }

    @Override
    public boolean readBooleanOption(Identifier id) {
        return this.readBooleanOption(id, true);
    }

    @Override
    public int readIntOption(Identifier id) {
        return this.readIntOption(id, true);
    }

    @Override
    public <E extends Enum<E>> E readEnumOption(Identifier id, Class<E> enumClass) {
        return this.readEnumOption(id, enumClass, true);
    }
}
