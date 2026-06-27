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

package net.caffeinemc.mods.sodium.client.render.frapi.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import java.util.function.Consumer;

public abstract class AbstractRenderContext implements RenderContext {
    private static final QuadTransform NO_TRANSFORM = q -> true;

    private QuadTransform activeTransform = NO_TRANSFORM;
    private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();
    private final QuadTransform stackTransform = q -> {
        int i = this.transformStack.size() - 1;

        while (i >= 0) {
            if (!this.transformStack.get(i--).transform(q)) {
                return false;
            }
        }

        return true;
    };

    @Deprecated
    private final Consumer<Mesh> meshConsumer = mesh -> mesh.outputTo(getEmitter());

    protected final boolean transform(MutableQuadView q) {
        return this.activeTransform.transform(q);
    }

    @Override
    public boolean hasTransform() {
        return this.activeTransform != NO_TRANSFORM;
    }

    @Override
    public void pushTransform(QuadTransform transform) {
        if (transform == null) {
            throw new NullPointerException("Renderer received null QuadTransform.");
        }

        this.transformStack.push(transform);

        if (this.transformStack.size() == 1) {
            this.activeTransform = transform;
        } else if (this.transformStack.size() == 2) {
            this.activeTransform = this.stackTransform;
        }
    }

    @Override
    public void popTransform() {
        this.transformStack.pop();

        if (this.transformStack.isEmpty()) {
            this.activeTransform = NO_TRANSFORM;
        } else if (this.transformStack.size() == 1) {
            this.activeTransform = this.transformStack.get(0);
        }
    }

    // Overridden to prevent allocating a lambda every time this method is called.
    @Deprecated
    @Override
    public Consumer<Mesh> meshConsumer() {
        return this.meshConsumer;
    }
}
