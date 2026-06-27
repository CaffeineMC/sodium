package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;

/**
 * This mixin is used to fix Forge's item models having drastic seams with Sodium's changed shrink ratio.
 */
@Mixin(ClientHooks.class)
public class ClientHooksMixin {

}
