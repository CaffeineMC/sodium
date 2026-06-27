package net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_builder.sorting;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StagedVertexBuffer.Draw.class)
public interface DrawAccessor {
    @Accessor
    VertexSorting getQuadSorting();
}
