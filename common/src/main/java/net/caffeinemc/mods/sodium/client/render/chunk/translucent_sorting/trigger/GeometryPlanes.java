package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import java.util.Collection;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.minecraft.core.SectionPos;

/**
 * GeometryPlanes stores the NormalPlanes for different normals, both aligned
 * and unaligned.
 */
public class GeometryPlanes {
    private NormalPlanes[] alignedPlanes;
    private Object2ReferenceMap<Vector3fc, NormalPlanes> unalignedPlanes;
    private final Vector3f unalignedNormalScratch = new Vector3f();

    public NormalPlanes[] getAligned() {
        return this.alignedPlanes;
    }

    public NormalPlanes[] getAlignedOrCreate() {
        if (this.alignedPlanes == null) {
            this.alignedPlanes = new NormalPlanes[ModelQuadFacing.DIRECTIONS];
        }
        return this.alignedPlanes;
    }

    public Collection<NormalPlanes> getUnaligned() {
        if (this.unalignedPlanes == null) {
            return null;
        }
        return this.unalignedPlanes.values();
    }

    public Object2ReferenceMap<Vector3fc, NormalPlanes> getUnalignedOrCreate() {
        if (this.unalignedPlanes == null) {
            this.unalignedPlanes = new Object2ReferenceOpenHashMap<>();
        }
        return this.unalignedPlanes;
    }

    NormalPlanes getPlanesForNormal(NormalList normalList) {
        var normal = normalList.getNormal();
        if (normalList.isAligned()) {
            if (this.alignedPlanes == null) {
                return null;
            }
            return this.alignedPlanes[normalList.getAlignedDirection()];
        } else {
            if (this.unalignedPlanes == null) {
                return null;
            }
            return this.unalignedPlanes.get(normal);
        }
    }

    public void addAlignedPlane(SectionPos sectionPos, int direction, float distance) {
        var alignedDistances = this.getAlignedOrCreate();
        var normalPlanes = alignedDistances[direction];
        if (normalPlanes == null) {
            normalPlanes = new NormalPlanes(sectionPos, direction);
            alignedDistances[direction] = normalPlanes;
        }
        normalPlanes.addPlaneMember(distance);
    }

    public void addDoubleSidedAlignedPlane(SectionPos sectionPos, int axis, float distance) {
        this.addAlignedPlane(sectionPos, axis, distance);
        this.addAlignedPlane(sectionPos, axis + 3, -distance);
    }

    public void addUnalignedPlane(SectionPos sectionPos, Vector3fc normal, float distance) {
        var unalignedDistances = this.getUnalignedOrCreate();

        // create a copy of the vector where -0 is turned into +0,
        // this avoids non-equality when comparing normals where just the sign on 0 is different
        var cleanedNormal = this.cleanNormal(normal);

        var normalPlanes = unalignedDistances.get(cleanedNormal);
        if (normalPlanes == null) {
            normalPlanes = new NormalPlanes(sectionPos, new Vector3f(normal));

            // NOTE: importantly use the cleaned normal here, not the cleanedNormal, which is mutable
            unalignedDistances.put(normalPlanes.normal, normalPlanes);
        }
        normalPlanes.addPlaneMember(distance);
    }

    private Vector3f cleanNormal(Vector3fc normal) {
        var cleanedNormal = this.unalignedNormalScratch.set(normal);

        // convert any occurrences of 0 into +0, note that -0.0f == 0.0f
        if (cleanedNormal.x == 0.0f) {
            cleanedNormal.x = 0.0f;
        }
        if (cleanedNormal.y == 0.0f) {
            cleanedNormal.y = 0.0f;
        }
        if (cleanedNormal.z == 0.0f) {
            cleanedNormal.z = 0.0f;
        }
        return cleanedNormal;
    }

    public void addDoubleSidedUnalignedPlane(SectionPos sectionPos, Vector3fc normal, float distance) {
        this.addUnalignedPlane(sectionPos, normal, distance);
        this.addUnalignedPlane(sectionPos, normal.negate(new Vector3f()), -distance);
    }

    public void addQuadPlane(SectionPos sectionPos, TQuad quad) {
        var facing = quad.useQuantizedFacing();
        if (facing.isAligned()) {
            this.addAlignedPlane(sectionPos, facing.ordinal(), quad.getQuantizedDotProduct());
        } else {
            this.addUnalignedPlane(sectionPos, quad.getQuantizedNormal(), quad.getQuantizedDotProduct());
        }
    }

    private void prepareAndInsert(Object2ReferenceMap<Vector3fc, float[]> distancesByNormal) {
        if (this.alignedPlanes != null) {
            for (var normalPlanes : this.alignedPlanes) {
                if (normalPlanes != null) {
                    normalPlanes.prepareAndInsert(distancesByNormal);
                }
            }
        }
        if (this.unalignedPlanes != null) {
            for (var normalPlanes : this.unalignedPlanes.values()) {
                normalPlanes.prepareAndInsert(distancesByNormal);
            }
        }
    }

    public void prepareIntegration() {
        this.prepareAndInsert(null);
    }

    public Object2ReferenceMap<Vector3fc, float[]> prepareAndGetDistances() {
        var distancesByNormal = new Object2ReferenceOpenHashMap<Vector3fc, float[]>(10);
        this.prepareAndInsert(distancesByNormal);
        return distancesByNormal;
    }

    public static GeometryPlanes fromQuadLists(SectionPos sectionPos, TQuad[] quads) {
        var geometryPlanes = new GeometryPlanes();
        for (var quad : quads) {
            geometryPlanes.addQuadPlane(sectionPos, quad);
        }
        return geometryPlanes;
    }
}
