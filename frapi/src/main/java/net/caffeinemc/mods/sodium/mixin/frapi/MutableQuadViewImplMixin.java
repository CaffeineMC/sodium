package net.caffeinemc.mods.sodium.mixin.frapi;

import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.MutableQuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.QuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MutableQuadViewImpl.class)
public class MutableQuadViewImplMixin implements ExtendedMutableQuadViewImpl {
    @Unique
    private MutableQuadViewWrapper wrapper;

    @Override
    public MutableQuadViewWrapper getWrapper() {
        if (wrapper == null) wrapper = new MutableQuadViewWrapper((MutableQuadViewImpl) (Object) this);
        return wrapper;
    }
}
