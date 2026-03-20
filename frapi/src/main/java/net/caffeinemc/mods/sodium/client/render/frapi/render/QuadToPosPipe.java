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

import java.util.function.Consumer;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;

public class QuadToPosPipe implements Consumer<QuadView> {
    private final Consumer<Vector3fc> posConsumer;
    private final Vector3f vec;
    public Matrix4fc matrix;

    public QuadToPosPipe(Consumer<Vector3fc> posConsumer, Vector3f vec) {
        this.posConsumer = posConsumer;
        this.vec = vec;
    }

    @Override
    public void accept(QuadView quad) {
        for (int i = 0; i < 4; i++) {
            quad.copyPos(i, vec);

            vec.x = MatrixHelper.transformPositionX(matrix, vec.x, vec.y, vec.z);
            vec.y = MatrixHelper.transformPositionY(matrix, vec.x, vec.y, vec.z);
            vec.z = MatrixHelper.transformPositionZ(matrix, vec.x, vec.y, vec.z);

            posConsumer.accept(vec);
        }
    }
}