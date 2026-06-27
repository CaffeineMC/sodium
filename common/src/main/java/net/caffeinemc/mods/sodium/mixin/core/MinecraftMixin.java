package net.caffeinemc.mods.sodium.mixin.core;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.checks.ResourcePackScanner;
import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.platform.windows.api.Imm32;
import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    /**
     * Check for problematic core shader resource packs after the initial game launch.
     */
    @Inject(method = "onGameLoadFinished", at = @At("HEAD"))
    private void postInit(GameLoadCookie cookie, CallbackInfo ci) {
        ResourcePackScanner.checkIfCoreShaderLoaded(this.resourceManager);

        ConfigManager.registerConfigsLate();
    }

    /**
     * Check for problematic core shader resource packs after every resource reload.
     */
    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("TAIL"))
    private void postResourceReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        ResourcePackScanner.checkIfCoreShaderLoaded(this.resourceManager);
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;registerTextures(Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
    private void registerSodiumIcon(TextureManager textureManager, Operation<Void> original) {
        SodiumConfigBuilder.registerIcon(textureManager);
        original.call(textureManager);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;<init>(Ljava/io/File;Lcom/mojang/datafixers/DataFixer;)V"))
    private void setFullscreen(GameConfig gameConfig, CallbackInfo ci) {
        if (!SodiumClientMod.options().notifications.hasEditedFullscreenOption) {
            SodiumClientMod.options().notifications.hasEditedFullscreenOption = true;
            try {
                SodiumOptions.writeToDisk(SodiumClientMod.options());
            } catch (IOException e) {
                SodiumClientMod.logger()
                        .error("Failed to update config file", e);
                return; // Do not get stuck in a loop of setting exclusive fullscreen! That'd be very annoying.
            }

            var hasIME = OsUtils.getOs() == OsUtils.OperatingSystem.WIN && Imm32.CheckIMEStatus();
            Minecraft.getInstance().options.exclusiveFullscreen().set(!hasIME);

            if (hasIME) {
                SodiumClientMod.logger().info("Setting exclusive fullscreen to false by default, as the user is using Japanese/Chinese/Korean and likely needs IME support.");
            } else {
                if (OsUtils.getOs() != OsUtils.OperatingSystem.WIN) {
                    SodiumClientMod.logger().info("Setting exclusive fullscreen to true by default, as the user is not on Windows and the language cannot be guessed.");
                } else {
                    SodiumClientMod.logger().info("Setting exclusive fullscreen to true by default, as the user is using a language that likely does not need an IME.");
                }
            }
        }
    }
}
