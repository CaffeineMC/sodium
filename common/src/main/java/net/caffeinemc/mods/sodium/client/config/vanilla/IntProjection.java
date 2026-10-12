package net.caffeinemc.mods.sodium.client.config.vanilla;

/**
 * This class is intended for conversion between double-valued vanilla options and our integer options.
 *
 * @param <T> The type of the values being projected to integers
 */
public interface IntProjection<T> {
    int toInt(T value);

    T fromInt(int value);

    static IntProjection<Integer> identity() {
        return new IntProjection<>() {
            @Override
            public int toInt(Integer value) {
                return value;
            }

            @Override
            public Integer fromInt(int value) {
                return value;
            }
        };
    }

    static IntProjection<Double> scaled(double scale) {
        return new IntProjection<>() {
            @Override
            public int toInt(Double value) {
                return (int) Math.round(value * scale);
            }

            @Override
            public Double fromInt(int value) {
                return value / scale;
            }
        };
    }
}
