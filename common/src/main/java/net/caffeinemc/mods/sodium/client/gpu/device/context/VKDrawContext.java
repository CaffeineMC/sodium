package net.caffeinemc.mods.sodium.client.gpu.device.context;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.RenderPass;
import org.lwjgl.vulkan.VkCommandBuffer;

public abstract class VKDrawContext extends DrawContext {
    @Override
    public void setContext(RenderPass pass, RenderPipeline pipeline) {
        this.pass = pass;
    }
}
