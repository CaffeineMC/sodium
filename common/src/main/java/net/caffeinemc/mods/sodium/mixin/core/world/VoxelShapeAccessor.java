package net.caffeinemc.mods.sodium.mixin.core.world;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VoxelShape.class)
public interface VoxelShapeAccessor {
    @Invoker("findIndex")
    int sodium$findIndex(Direction.Axis axis, double coord);
}
