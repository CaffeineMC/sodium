package net.caffeinemc.mods.sodium.client.config.vanilla;

import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.mixin.core.gui.OptionInstanceAccessor;
import net.caffeinemc.mods.sodium.mixin.core.gui.TooltipAccessor;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * A wrapper around a vanilla option instance that provides access to its properties and values.
 * Instances of this class are generated as proxies to the vanilla options.
 *
 * @param <T> The type of the option value
 */
public class VanillaOption<T> {
    private final OptionInstance<T> instance;
    private final OptionInstanceAccessor<T> accessor;

    @SuppressWarnings("unchecked")
    public VanillaOption(OptionInstance<T> instance) {
        this.instance = instance;
        this.accessor = (OptionInstanceAccessor<T>) (Object) instance;
    }

    public T get() {
        return this.instance.get();
    }

    public void set(T value) {
        this.instance.set(value);
    }

    public Component name() {
        return this.accessor.sodium$getCaption();
    }

    public T defaultValue() {
        return this.accessor.sodium$getInitialValue();
    }

    /**
     * Returns a function that provides a tooltip for the given value of the option.
     * If the option does not have a tooltip, this method returns null.
     *
     * @return A function that provides a tooltip for the given value of the option, or null if the option does not have a tooltip
     */
    public @Nullable Function<T, Component> getTooltipProvider() {
        // operates under the assumption that options have tooltips for the default value if they have any
        var defaultTooltip = this.getTooltip(this.defaultValue());
        if (defaultTooltip == null) {
            return null;
        }

        return value -> Objects.requireNonNullElse(this.getTooltip(value), defaultTooltip);
    }

    private @Nullable Component getTooltip(T value) {
        // get tooltip from the option through accessors if there is one
        var tooltip = this.accessor.sodium$getTooltip().apply(value);
        return tooltip == null ? null : ((TooltipAccessor) tooltip).sodium$getMessage();
    }

    public Component getElementName(T value) {
        return this.accessor.sodium$getToString().apply(value);
    }

    public Range getRangeFor(IntProjection<T> projection) {
        if (!(this.instance.values() instanceof OptionInstance.SliderableValueSet<T> values)) {
            throw new IllegalStateException("Option " + this.instance + " is not a slider option");
        }

        T first = values.fromSliderValue(0.0);
        int min = projection.toInt(first);
        int max = projection.toInt(values.fromSliderValue(1.0));

        // figure out the step size in the projected integer space by checking adjacent values
        int step = values.next(first).map(next -> projection.toInt(next) - min).orElse(1);

        return new Range(min, max, step);
    }
}
