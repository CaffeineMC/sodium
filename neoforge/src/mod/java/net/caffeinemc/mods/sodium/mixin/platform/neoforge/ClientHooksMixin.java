package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * This mixin is used to fix Forge's item models having drastic seams with Sodium's changed shrink ratio.
 */
@Mixin(ClientHooks.class)
public class ClientHooksMixin {

}
