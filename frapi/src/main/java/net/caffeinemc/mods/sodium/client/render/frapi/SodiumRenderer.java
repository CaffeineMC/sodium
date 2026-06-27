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

package net.caffeinemc.mods.sodium.client.render.frapi;

import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableMeshImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.MutableQuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.model.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.minecraft.client.color.block.BlockColors;

import java.util.function.Consumer;

/**
 * The Sodium renderer implementation.
 */
public class SodiumRenderer implements Renderer {
    public static final SodiumRenderer INSTANCE = new SodiumRenderer();
    public static final Consumer<MutableQuadViewImpl> BUFFERER = quad -> ((ExtendedMutableQuadViewImpl) quad).getWrapper().transformAndEmit();

    private SodiumRenderer() { }

    @Override
    public QuadEmitter quadEmitter(Consumer<? super MutableQuadView> consumer) {
        MutableQuadViewWrapper wrapper = new MutableQuadViewWrapper(null);

        MutableQuadViewImpl impl = new MutableQuadViewImpl() {
            {
                this.data = new int[EncodingFormat.TOTAL_STRIDE];
                this.clear();
            }

            @Override
            public void emitDirectly() {
                consumer.accept(wrapper);
            }
        };

        wrapper.setDelegate(impl);

        return wrapper;
    }

    @Override
    public MutableMesh mutableMesh() {
        return new MutableMeshImpl();
    }

    @Override
    public AltModelBlockRenderer altModelBlockRenderer(boolean ambientOcclusion, boolean cull, BlockColors blockColors) {
        return new NonTerrainBlockRenderContext(ambientOcclusion, cull, blockColors);
    }


}
