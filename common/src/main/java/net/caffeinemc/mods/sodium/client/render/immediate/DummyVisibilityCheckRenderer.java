package net.caffeinemc.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;

public class DummyVisibilityCheckRenderer {

    private static final ShaderProgram DUMMY_VISIBILITY_CHECK_SHADER = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("sodium", "dummy_visibility_check"),
            DefaultVertexFormat.POSITION,
            ShaderDefines.builder().build()
    );

    private static VertexBuffer vertexBuffer = null;

    public static void setup(Matrix4f projectionMatrix) {
        if (vertexBuffer == null) {
            vertexBuffer = rebuildGeometry();
        }

        RenderSystem.setShader(DUMMY_VISIBILITY_CHECK_SHADER);

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        vertexBuffer.bind();

        CompiledShaderProgram program = RenderSystem.getShader();
        program.setDefaultUniforms(VertexFormat.Mode.QUADS, new Matrix4f(), projectionMatrix, Minecraft.getInstance().getWindow());
        program.apply();
    }

    public static void render(double x, double y, double z, Matrix4f modelView) {
        Matrix4f modelViewMatrix = new Matrix4f(modelView);
        modelViewMatrix.translate((float) x, (float) y, (float) z);

        CompiledShaderProgram program = RenderSystem.getShader();
        if (program.MODEL_VIEW_MATRIX != null) {
            program.MODEL_VIEW_MATRIX.set(modelViewMatrix);
            program.MODEL_VIEW_MATRIX.upload();
        }
        vertexBuffer.draw();
    }

    public static void teardown() {
        CompiledShaderProgram program = RenderSystem.getShader();
        program.clear();

        VertexBuffer.unbind();

        // State teardown
        RenderSystem.depthFunc(GL32C.GL_LEQUAL);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
    }

    private static VertexBuffer rebuildGeometry()
    {
        BufferBuilder bufferBuilder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        var writer = VertexBufferWriter.of(bufferBuilder);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final int vertexCount = 6 * 4;
            final long vertexBuffer = stack.nmalloc(vertexCount * 12);

            long ptr = vertexBuffer;

            final float x0 = -4;
            final float y0 = -4;
            final float z0 = -4;

            final float x1 = 20;
            final float y1 = 20;
            final float z1 = 20;

            // -Y
            PositionAttribute.put(ptr + 0, x1, y0, z1);
            PositionAttribute.put(ptr + 12, x0, y0, z1);
            PositionAttribute.put(ptr + 24, x0, y0, z0);
            PositionAttribute.put(ptr + 36, x1, y0, z0);
            ptr += 48;

            // +Y
            PositionAttribute.put(ptr + 0, x0, y1, z1);
            PositionAttribute.put(ptr + 12, x1, y1, z1);
            PositionAttribute.put(ptr + 24, x1, y1, z0);
            PositionAttribute.put(ptr + 36, x0, y1, z0);
            ptr += 48;

            // -X
            PositionAttribute.put(ptr + 0, x0, y0, z1);
            PositionAttribute.put(ptr + 12, x0, y1, z1);
            PositionAttribute.put(ptr + 24, x0, y1, z0);
            PositionAttribute.put(ptr + 36, x0, y0, z0);
            ptr += 48;

            // +X
            PositionAttribute.put(ptr + 0, x1, y1, z1);
            PositionAttribute.put(ptr + 12, x1, y0, z1);
            PositionAttribute.put(ptr + 24, x1, y0, z0);
            PositionAttribute.put(ptr + 36, x1, y1, z0);
            ptr += 48;

            // -Z
            PositionAttribute.put(ptr + 0, x1, y1, z0);
            PositionAttribute.put(ptr + 12, x1, y0, z0);
            PositionAttribute.put(ptr + 24, x0, y0, z0);
            PositionAttribute.put(ptr + 36, x0, y1, z0);
            ptr += 48;

            // +Z
            PositionAttribute.put(ptr + 0, x1, y0, z1);
            PositionAttribute.put(ptr + 12, x1, y1, z1);
            PositionAttribute.put(ptr + 24, x0, y1, z1);
            PositionAttribute.put(ptr + 36, x0, y0, z1);
            ptr += 48;

            writer.push(stack, vertexBuffer, vertexCount, DefaultVertexFormat.POSITION);
        }

        MeshData meshData = bufferBuilder.buildOrThrow();

        VertexBuffer vertexBuffer = new VertexBuffer(BufferUsage.DYNAMIC_WRITE);

        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
        VertexBuffer.unbind();

        Tesselator.getInstance().clear();

        return vertexBuffer;
    }

}
