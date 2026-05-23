package net.caffeinemc.mods.sodium.client.util;

import net.minecraft.TracingExecutor;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class IgnoringSectionRenderDispatcher extends SectionRenderDispatcher {
    public IgnoringSectionRenderDispatcher(TracingExecutor executor, RenderBuffers renderBuffers, SectionCompiler sectionCompiler, Consumer<RenderSection> onSectionMeshUpdate) {
        super(executor, renderBuffers, sectionCompiler, onSectionMeshUpdate);
        super.dispose();
    }

    @Override
    public void setCompiler(SectionCompiler sectionCompiler) {

    }

    @Override
    public void setCameraPosition(Vec3 cameraPosition) {

    }

    @Override
    public @Nullable RenderSectionBufferSlice getRenderSectionSlice(SectionMesh sectionMesh, ChunkSectionLayer layer) {
        return null;
    }

    @Override
    public void lock() {

    }

    @Override
    public void unlock() {

    }

    @Override
    public void uploadTerrainBuffersToGpu() {
    }

    @Override
    public void clearCompileQueue() {
    }

    @Override
    public boolean isQueueEmpty() {
        return true;
    }

    @Override
    public void dispose() {

    }

    @Override
    public String getStats() {
        return "None";
    }

    @Override
    public int getCompileQueueSize() {
        return 0;
    }

    @Override
    public int getFreeBufferCount() {
        return 0;
    }
}
