package net.caffeinemc.mods.sodium.client.gpu.device.context;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.mixin.core.GlRenderPassAccessor;
import net.caffeinemc.mods.sodium.mixin.core.RenderPassAccessor;
import org.lwjgl.opengl.GL46C;

public class GLDrawContext extends DrawContext {
    private int regionUniform;
    private int timeUniform;
    private int idUniform;

    @Override
    public void setContext(RenderPass pass, RenderPipeline pipeline) {
        this.pass = pass;
        GlRenderPassAccessor passBackend = (GlRenderPassAccessor) ((RenderPassAccessor) pass).getBackend();
        int programId = passBackend.getPipeline().program().getProgramId();
        GlStateManager._glUseProgram(programId);
        this.regionUniform = GL46C.glGetUniformLocation(programId, "u_RegionOffset");
        this.timeUniform = GL46C.glGetUniformLocation(programId, "u_CurrentTime");
        this.idUniform = GL46C.glGetUniformLocation(programId, "u_RegionID");
        GlStateManager._glUseProgram(programId);
    }

    @Override
    public void updateData(RenderRegion region, CameraTransform camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
        float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
        float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);

        GL46C.glUniform3f(this.regionUniform, x, y, z);
        GL46C.glUniform1i(this.timeUniform, Math.toIntExact(System.currentTimeMillis() - region.getCreationTime()));
        GL46C.glUniform1ui(this.idUniform, region.getId());
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
}
