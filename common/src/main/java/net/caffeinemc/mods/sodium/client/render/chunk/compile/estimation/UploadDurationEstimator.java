package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UploadDurationEstimator extends ExpDecayLinear2DEstimator<Void> {
    public static final double NEW_DATA_RATIO = 0.05;
    public static final int INITIAL_SAMPLE_TARGET = 100;
    public static final int MIN_BATCH_SIZE = 100;
    private static final long INITIAL_UPLOAD_TIME_ESTIMATE = 100_000L; // 100µs

    public UploadDurationEstimator() {
        super(NEW_DATA_RATIO, INITIAL_SAMPLE_TARGET, MIN_BATCH_SIZE, INITIAL_UPLOAD_TIME_ESTIMATE);
    }

    public long estimateUploadDuration(long size) {
        return this.predict(null, size);
    }
    
    // special map that can contain one key: null and a generic value type
    private static class VoidKeyMap<T> implements Map<Void, T> {
        private T value;

        @Override
        public int size() {
            return this.value == null ? 0 : 1;
        }

        @Override
        public boolean isEmpty() {
            return this.value == null;
        }

        @Override
        public boolean containsKey(Object o) {
            return o == null;
        }

        @Override
        public boolean containsValue(Object o) {
            return this.value != null && this.value.equals(o);
        }

        @Override
        public T get(Object o) {
            if (o == null) {
                return this.value;
            }
            return null;
        }

        @Override
        public @Nullable T put(Void unused, T t) {
            T oldValue = this.value;
            this.value = t;
            return oldValue;
        }

        @Override
        public T remove(Object o) {
            if (o == null) {
                T oldValue = this.value;
                this.value = null;
                return oldValue;
            }
            return null;
        }

        @Override
        public void putAll(@NotNull Map<? extends Void, ? extends T> map) {
            if (map.containsKey(null)) {
                this.value = map.get(null);
            }
        }

        @Override
        public void clear() {
            this.value = null;
        }

        @Override
        public @NotNull Set<Void> keySet() {
            if (this.value != null) {
                return Collections.singleton(null);
            }
            return Set.of();
        }

        @Override
        public @NotNull Collection<T> values() {
            if (this.value != null) {
                return Collections.singleton(this.value);
            }
            return Collections.emptyList();
        }

        @Override
        public @NotNull Set<Entry<Void, T>> entrySet() {
            if (this.value != null) {
                return Collections.singleton(new AbstractMap.SimpleEntry<>(null, this.value));
            }
            return Collections.emptySet();
        }
    }

    @Override
    protected <T> Map<Void, T> createMap() {
        return new VoidKeyMap<>();
    }
}
