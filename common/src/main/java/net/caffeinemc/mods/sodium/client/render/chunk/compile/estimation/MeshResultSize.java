package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;

public record MeshResultSize(SectionCategory category, long resultSize) implements Average1DEstimator.Value<MeshResultSize.SectionCategory> {
    public static long NO_DATA = -1;

    public enum SectionCategory {
        LOW,
        UNDERGROUND,
        WATER_LEVEL,
        SURFACE,
        HIGH;

        public static SectionCategory forSection(RenderSection section, int seaLevelChunk) {
            var sectionY = section.getChunkY();
            
            // Roughly classify type of chunk based on Y level relative to sea level:
            // Water level chunks are likely to have different meshes from those below and those above.
            // Very low chunks are again different because they aren't going to include the underwater terrain (just caves).
            // Very high chunks are (at least locally) different because they are likely to be mostly air with some terrain poking through, or just buildings/jungle tree tops.
            if (sectionY == seaLevelChunk) {
                return WATER_LEVEL;
            }
            if (sectionY < seaLevelChunk - 4) {
                return LOW;
            }
            if (sectionY < seaLevelChunk) {
                return UNDERGROUND;
            }
            if (sectionY < seaLevelChunk + 3) {
                return SURFACE;
            }
            return HIGH;
        }
    }

    @Override
    public SectionCategory category() {
        return this.category;
    }

    @Override
    public long value() {
        return this.resultSize;
    }
}
