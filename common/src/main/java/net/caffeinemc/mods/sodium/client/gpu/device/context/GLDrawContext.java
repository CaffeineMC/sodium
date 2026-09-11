package net.caffeinemc.mods.sodium.client.gpu.device.context;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.RenderPass;

public class GLDrawContext extends DrawContext {
    @Override
    public void setContext(RenderPass pass, RenderPipeline pipeline) {
        this.pass = pass;
    }

    @Override
    public void rotate() {

    }

    @Override
    public void delete() {

    }

    @Override
    public void endDraw() {

    }

    @Override
    public String name() {
        return "gl_multidraw (sodium)";
    }
}
