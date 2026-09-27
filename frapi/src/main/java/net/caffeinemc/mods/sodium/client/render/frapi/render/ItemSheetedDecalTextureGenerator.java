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

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

class ItemSheetedDecalTextureGenerator implements VertexConsumer {
	private VertexConsumer delegate;
	private Matrix4fc cameraInversePose;
	private Matrix3fc normalInversePose;
	private float textureScale;

	private final Vector3f worldPos = new Vector3f();
	private final Vector3f normal = new Vector3f();

	private float x;
	private float y;
	private float z;

	public void prepare(VertexConsumer delegate, Matrix4fc cameraInversePose, Matrix3fc normalInversePose, float textureScale) {
		this.delegate = delegate;
		this.cameraInversePose = cameraInversePose;
		this.normalInversePose = normalInversePose;
		this.textureScale = textureScale;
	}

	public void clear() {
		delegate = null;
		cameraInversePose = null;
		normalInversePose = null;
	}

	@Override
	public VertexConsumer addVertex(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		delegate.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(final int r, final int g, final int b, final int a) {
		delegate.setColor(r, g, b, a);
		return this;
	}

	@Override
	public VertexConsumer setColor(final int color) {
		delegate.setColor(color);
		return this;
	}

	@Override
	public VertexConsumer setUv(final float u, final float v) {
		delegate.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(final int u, final int v) {
		delegate.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(final int u, final int v) {
		delegate.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv3(final float u, final float v) {
		return this;
	}

	@Override
	public VertexConsumer setNormal(final float x, final float y, final float z) {
		delegate.setNormal(x, y, z);
		Vector3f normal = normalInversePose.transform(x, y, z, this.normal);
		Direction direction = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());
		Vector3f worldPos = cameraInversePose.transformPosition(this.x, this.y, this.z, this.worldPos);
		worldPos.rotateY(Mth.PI);
		worldPos.rotateX(-Mth.HALF_PI);
		worldPos.rotate(direction.getRotation());
		delegate.setUv3(-worldPos.x() * textureScale, -worldPos.y() * textureScale);
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(final float width) {
		delegate.setLineWidth(width);
		return this;
	}
}
