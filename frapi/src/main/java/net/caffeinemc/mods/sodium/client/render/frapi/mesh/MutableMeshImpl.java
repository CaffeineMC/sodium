/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.caffeinemc.mods.sodium.client.render.frapi.mesh;

import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;

import java.util.function.Consumer;

/**
 * Our implementation of {@link MutableMesh}, used for static mesh creation and baking.
 * Not much to it - mainly it just needs to grow the int[] array as quads are appended
 * and maintain/provide a properly-configured {@link net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView} instance.
 * All the encoding and other work is handled in the quad base classes.
 * The one interesting bit is in {@link MutableQuadViewImpl#emitDirectly()}.
 */
public class MutableMeshImpl extends MeshViewImpl implements MutableMesh {
    private final MutableQuadViewImpl emitter = new MutableQuadViewImpl() {
    @Override
    public void emitDirectly() {
        // Necessary because the validity of geometry is not encoded; reading mesh data always
        // uses QuadViewImpl#load(), which assumes valid geometry. Built immutable meshes
        // should also have valid geometry for better performance.
        this.computeGeometry();
        MutableMeshImpl.this.limit += EncodingFormat.TOTAL_STRIDE;
        MutableMeshImpl.this.ensureCapacity(EncodingFormat.TOTAL_STRIDE);
        this.baseIndex = MutableMeshImpl.this.limit;
    }
};

    public MutableMeshImpl() {
        this.data = new int[8 * EncodingFormat.TOTAL_STRIDE];
        this.limit = 0;

        this.ensureCapacity(EncodingFormat.TOTAL_STRIDE);
        this.emitter.data = this.data;
        this.emitter.baseIndex = this.limit;
        this.emitter.clear();
    }

    private void ensureCapacity(int stride) {
        if (stride > this.data.length - this.limit) {
            final int[] bigger = new int[this.data.length * 2];
            System.arraycopy(this.data, 0, bigger, 0, this.limit);
            this.data = bigger;
            this.emitter.data = this.data;
        }
    }

    @Override
    public QuadEmitter emitter() {
        this.emitter.clear();
        return ((ExtendedMutableQuadViewImpl) this.emitter).getWrapper();
    }

    @Override
    public void forEachMutable(Consumer<? super MutableQuadView> action) {
        // emitDirectly will not be called by forEach, so just reuse the main emitter.
        this.forEach((a) -> action.accept((MutableQuadView) a), ((ExtendedMutableQuadViewImpl) this.emitter).getWrapper()); // TODO: probably wrong
        this.emitter.data = this.data;
        this.emitter.baseIndex = this.limit;
    }

    @Override
    public Mesh immutableCopy() {
        final int[] packed = new int[this.limit];
        System.arraycopy(this.data, 0, packed, 0, this.limit);
        return new MeshImpl(packed);
    }

    @Override
    public void clear() {
        this.limit = 0;
        this.emitter.baseIndex = this.limit;
        this.emitter.clear();
    }
}
