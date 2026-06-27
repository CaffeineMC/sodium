package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;

/**
 * Self-mixin that implements {@link RenderContext} with NeoForge's {@link ModelData} based on
 * {@link SodiumModelData} state in {@link AbstractBlockRenderContext}.
 *
 * See <a href="https://github.com/CaffeineMC/sodium/issues/3517">#3517</a>.
 */
@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractBlockRenderContextMixin implements RenderContext {
    @Shadow
    protected RenderType type;

    @Shadow
    protected SodiumModelData modelData;

    @Shadow
    @Final
    protected Deque<SodiumModelData> modelDataStack;

    @Shadow
    protected TriState useAO;

    @Override
    public ModelData getModelData() {
        SodiumModelData top = this.modelDataStack.peek();
        return (ModelData) (Object) (top != null ? top : this.modelData);
    }

    @Override
    public RenderType getRenderType() {
        return this.type;
    }

    @Override
    public void pushModelData(ModelData modelData) {
        // ModelData implements SodiumModelData via ModelDataMixin
        this.modelDataStack.push((SodiumModelData) (Object) modelData);
    }

    @Override
    public void popModelData() {
        this.modelDataStack.pop();
    }

    @Override
    public TriState usesAmbientOcclusion() {
        return this.useAO;
    }

    @Override
    public void setUsesAmbientOcclusion(TriState state) {
        this.useAO = state;
    }
}
