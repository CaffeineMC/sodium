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

public abstract class VKDrawContext extends DrawContext {
    protected VkCommandBuffer cmdBuf;
    protected long layout;

    @Override
    public void updateData(RenderRegion region, CameraTransform camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
        float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
        float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long memory = stack.nmalloc(PUSH_CONSTANT_RANGE);
            MemoryUtil.memPutFloat(memory, x);
            MemoryUtil.memPutFloat(memory + 4, y);
            MemoryUtil.memPutFloat(memory + 8, z);
            MemoryUtil.memPutInt(memory + 12, Math.toIntExact(System.currentTimeMillis() - region.getCreationTime()));
            MemoryUtil.memPutInt(memory + 16, region.getId());

            VK13.nvkCmdPushConstants(cmdBuf, layout, VK13.VK_SHADER_STAGE_ALL, 0, PUSH_CONSTANT_RANGE, memory);
        }
    }

    @Override
    public void setContext(RenderPass pass, RenderPipeline pipeline) {
        this.pass = pass;
        VulkanRenderPassAccessor passBackend = (VulkanRenderPassAccessor) ((RenderPassAccessor) pass).getBackend();
        this.layout = passBackend.sodium$getPipeline().pipelineLayout();
        this.cmdBuf = passBackend.sodium$getCommandBuffer();
    }
}
