package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.compatibility;

import net.caffeinemc.mods.sodium.client.checks.ResourcePackScanner;
import net.caffeinemc.mods.sodium.client.gui.screen.ResourcePackIssuesScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ TitleScreen.class, PauseScreen.class })
public abstract class ProblemIndicatorMixin extends Screen {
    @Unique
    private @Nullable Button sodium$problemIndicator;

    protected ProblemIndicatorMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sodium$addProblemIndicator(CallbackInfo ci) {
        this.sodium$problemIndicator = this.addRenderableWidget(Button.builder(
                        Component.empty(),
                        button -> this.minecraft.gui.setScreen(new ResourcePackIssuesScreen(this)))
                .bounds(Math.max(4, this.width - 184), 4, 180, 20)
                .tooltip(Tooltip.create(Component.translatable("sodium.compatibility_issues.indicator.tooltip")))
                .build());

        this.sodium$updateProblemIndicator();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sodium$updateProblemIndicator(CallbackInfo ci) {
        this.sodium$updateProblemIndicator();
    }

    @Unique
    private void sodium$updateProblemIndicator() {
        if (this.sodium$problemIndicator == null) {
            return;
        }

        var problems = ResourcePackScanner.getCurrentProblems();
        this.sodium$problemIndicator.visible = !problems.isEmpty();

        if (!problems.isEmpty()) {
            var color = ResourcePackScanner.hasSevereProblems() ? ChatFormatting.RED : ChatFormatting.GOLD;
            this.sodium$problemIndicator.setMessage(Component.translatable(
                            "sodium.compatibility_issues.indicator", problems.size())
                    .withStyle(color));
        }
    }
}
