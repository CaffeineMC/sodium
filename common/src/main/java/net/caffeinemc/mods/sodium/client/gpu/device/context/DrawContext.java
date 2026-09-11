package net.caffeinemc.mods.sodium.client.gpu.device.context;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;

public abstract class DrawContext {
    protected RenderPass pass;

    public static DrawContext create() {
        if (DrawBackend.BACKEND == DrawBackend.OPENGL) {
            return new GLDrawContext();
        } else if (DrawBackend.BACKEND == DrawBackend.VK_MULTIDRAW) {
            return new VKMultiDrawContext();
        } else if (DrawBackend.BACKEND == DrawBackend.VK_INDIRECT) {
            return new VKIndirectContext();
        }

        throw new IllegalStateException("Unknown backend");
    }

    public RenderPass getPass() {
        return this.pass;
    }

    public abstract void setContext(RenderPass pass, RenderPipeline pipeline);

    public abstract void rotate();

    public abstract void delete();

    public abstract void endDraw();

    public abstract String name();
}
