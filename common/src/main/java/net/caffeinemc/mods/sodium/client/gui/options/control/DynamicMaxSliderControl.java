package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.apache.commons.lang3.Validate;

import java.util.function.IntSupplier;

public class DynamicMaxSliderControl implements Control<Integer> {
    private final Option<Integer> option;

    private final IntSupplier max;
    private final int min, interval;

    private final ControlValueFormatter mode;

    public DynamicMaxSliderControl(Option<Integer> option, int min, IntSupplier max, int interval, ControlValueFormatter mode) {
        Validate.isTrue(interval > 0, "The slider interval must be greater than zero");
        Validate.notNull(mode, "The slider mode must not be null");

        this.option = option;
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.mode = mode;
    }

    @Override
    public ControlElement<Integer> createElement(Dim2i dim) {
        int min = this.min;
        int max = this.max.getAsInt();
        int interval = this.interval;

        Validate.isTrue(max > min, "The maximum value must be greater than the minimum value");
        Validate.isTrue(((max - min) % interval) == 0, "The maximum value must be divisable by the interval");

        return new SliderControl.Button(this.option, dim, min, max, interval, this.mode);
    }

    @Override
    public Option<Integer> getOption() {
        return this.option;
    }

    @Override
    public int getMaxWidth() {
        return 170;
    }
}
