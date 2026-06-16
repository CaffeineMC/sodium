package net.caffeinemc.mods.sodium.client.gpu.device.context;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.mixin.core.RenderPassAccessor;
import net.caffeinemc.mods.sodium.mixin.core.VulkanRenderPassAccessor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;

public class VKMultiDrawContext extends VKDrawContext {
    @Override
    public void rotate() {

    }

    @Override
    public void delete() {

    }

    @Override
    public void endDraw() {

    }
}
