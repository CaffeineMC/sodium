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

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

/**
 * Our implementation of {@link MeshBuilder}, used for static mesh creation and baking.
 * Not much to it - mainly it just needs to grow the int[] array as quads are appended
 * and maintain/provide a properly-configured {@link net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView} instance.
 * All the encoding and other work is handled in the quad base classes.
 * The one interesting bit is in {@link Maker#emitDirectly()}.
 */
public class MeshBuilderImpl implements MeshBuilder {
    private int[] data = new int[256];
    private int index = 0;
    private int limit = this.data.length;
    private final Maker maker = new Maker();

    public MeshBuilderImpl() {
        this.ensureCapacity(EncodingFormat.TOTAL_STRIDE);
        this.maker.data = this.data;
        this.maker.baseIndex = this.index;
        this.maker.clear();
    }

    protected void ensureCapacity(int stride) {
        if (stride > this.limit - this.index) {
            this.limit *= 2;
            final int[] bigger = new int[this.limit];
            System.arraycopy(this.data, 0, bigger, 0, this.index);
            this.data = bigger;
            this.maker.data = this.data;
        }
    }

    @Override
    public QuadEmitter getEmitter() {
        this.maker.clear();
        return this.maker;
    }

    @Override
    public Mesh build() {
        final int[] packed = new int[this.index];
        System.arraycopy(this.data, 0, packed, 0, this.index);
        this.index = 0;
        this.maker.baseIndex = this.index;
        this.maker.clear();
        return new MeshImpl(packed);
    }

    /**
     * Our base classes are used differently so we define final
     * encoding steps in subtypes. This will be a static mesh used
     * at render time so we want to capture all geometry now and
     * apply non-location-dependent lighting.
     */
    private class Maker extends MutableQuadViewImpl {
        @Override
        public void emitDirectly() {
            this.computeGeometry();
            MeshBuilderImpl.this.index += EncodingFormat.TOTAL_STRIDE;
            MeshBuilderImpl.this.ensureCapacity(EncodingFormat.TOTAL_STRIDE);
            this.baseIndex = MeshBuilderImpl.this.index;
        }
    }
}
