package net.caffeinemc.mods.sodium.client.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;

public class VertexFormatOffsetCache {
    private static final VertexFormatOffsetCache INSTANCE = new VertexFormatOffsetCache();

    public static VertexFormatOffsetCache getInstance() {
        return INSTANCE;
    }

    private static int getCacheSize() {
        return 6;
    }

    public static final int POSITION = 0;
    public static final int COLOR = 1;
    public static final int UV = 2;
    public static final int OVERLAY = 3;
    public static final int LIGHT = 4;
    public static final int NORMAL = 5;

    private static final String[] KEYS = {
            "Position",
            "Color",
            "UV0",
            "UV1",
            "UV2",
            "Normal"
    };

    private final Map<VertexFormat, int[]> offsetCache = new Reference2ReferenceOpenHashMap<>();

    public int[] getCachedOffsets(VertexFormat format) {
        return offsetCache.computeIfAbsent(format, f -> {
            int[] offsets = new int[6];

            for (int i = 0; i < 6; i++) {
                var key = KEYS[i];
                if (f.contains(key)) {
                    offsets[i] = f.getElement(key).offset();
                } else {
                    offsets[i] = -1;
                }
            }

            return offsets;
        });
    }
}
