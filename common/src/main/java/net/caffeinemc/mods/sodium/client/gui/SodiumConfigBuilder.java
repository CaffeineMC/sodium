package net.caffeinemc.mods.sodium.client.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatterImpls;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.minecraft.client.*;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

// TODO: get initialValue from the vanilla options (it's private)
public class SodiumConfigBuilder implements ConfigEntryPoint {
    private static final Identifier SODIUM_ICON = Identifier.fromNamespaceAndPath("sodium", "textures/gui/config-icon.png");
    private static final SodiumOptions DEFAULTS = SodiumOptions.defaults();

    private final Options vanillaOpts;
    private final StorageEventHandler vanillaStorage;
    private final SodiumOptions sodiumOpts;
    private final StorageEventHandler sodiumStorage;

    private final @Nullable Window window;
    private final Monitor monitor;

    public SodiumConfigBuilder() {
        var minecraft = Minecraft.getInstance();
        this.window = minecraft.getWindow();
        this.monitor = this.window == null ? null : this.window.findBestMonitor();

        this.vanillaOpts = minecraft.options;
        this.vanillaStorage = this.vanillaOpts == null ? null : () -> {
            this.vanillaOpts.save();

            SodiumClientMod.logger().info("Flushed changes to Minecraft configuration");
        };

        this.sodiumOpts = SodiumClientMod.options();
        this.sodiumStorage = () -> {
            try {
                SodiumOptions.writeToDisk(this.sodiumOpts);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't save configuration changes", e);
            }

            SodiumClientMod.logger().info("Flushed changes to Sodium configuration");
        };
    }

    public static void registerIcon(TextureManager textureManager) {
        textureManager.registerAndLoad(SODIUM_ICON, new SodiumLogo());
    }

    static class SodiumLogo extends ReloadableTexture {
        public SodiumLogo() {
            super(SODIUM_ICON);
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            try (InputStream inputStream = SodiumConfigBuilder.class.getResourceAsStream("/config-icon.png")) {
                return new TextureContents(NativeImage.read(inputStream), new TextureMetadataSection(false, false, MipmapStrategy.AUTO, 0.1f));
            }
        }
    }

    @Override
    public void registerConfigEarly(ConfigBuilder builder) {
        new SodiumConfigBuilder().buildEarlyConfig(builder);
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        new SodiumConfigBuilder().buildFullConfig(builder);
    }

    private static ModOptionsBuilder createModOptionsBuilder(ConfigBuilder builder) {
        return builder.registerOwnModOptions()
                .setName("Sodium")
                .setIcon(SODIUM_ICON)
                .formatVersion(version -> {
                    var result = version.splitWithDelimiters("\\+", 2);
                    return result[0];
                });
    }

    private void buildEarlyConfig(ConfigBuilder builder) {
        createModOptionsBuilder(builder).addPage(
                builder.createOptionPage()
                        .setName(Component.translatable("sodium.options.pages.performance"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .addOption(this.buildNoErrorContextOption(builder))));
    }

    private void buildFullConfig(ConfigBuilder builder) {
        createModOptionsBuilder(builder)
                .setColorTheme(builder.createColorTheme().setFullThemeRGB(
                        Colors.THEME, Colors.THEME_LIGHTER, Colors.THEME_DARKER))
                .addPage(this.buildGeneralPage(builder))
                .addPage(this.buildQualityPage(builder))
                .addPage(this.buildPerformancePage(builder))
                .addPage(this.buildAdvancedPage(builder));
    }

    private OptionPageBuilder buildGeneralPage(ConfigBuilder builder) {
        var generalPage = builder.createOptionPage().setName(Component.literal("General"));
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        // TODO: make RD option respect Vanilla's >16 RD only allowed if memory >1GB constraint
                        builder.createIntegerOption(Identifier.parse("sodium:general.render_distance"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.renderDistance"))
                                .setTooltip(Component.translatable("sodium.options.view_distance.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("options.chunks"))
                                .setRange(2, 32, 1)
                                .setDefaultValue(12)
                                .setBinding(this.vanillaOpts.renderDistance()::set, this.vanillaOpts.renderDistance()::get)
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:general.simulation_distance"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.simulationDistance"))
                                .setTooltip(Component.translatable("sodium.options.simulation_distance.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("options.chunks"))
                                .setRange(5, 32, 1)
                                .setDefaultValue(12)
                                .setBinding(this.vanillaOpts.simulationDistance()::set, this.vanillaOpts.simulationDistance()::get)
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:general.gamma"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.gamma"))
                                .setTooltip(Component.translatable("sodium.options.brightness.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.brightness())
                                .setRange(0, 100, 1)
                                .setDefaultValue(50)
                                .setBinding(value -> this.vanillaOpts.gamma().set(value * 0.01D), () -> (int) (this.vanillaOpts.gamma().get() / 0.01D))
                )
        );
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:general.gui_scale"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.guiScale"))
                                .setTooltip(Component.translatable("sodium.options.gui_scale.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.guiScale())
                                .setRangeProvider((state) -> new Range(0, this.window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode()), 1), ConfigState.UPDATE_ON_REBUILD)
                                .setDefaultValue(0)
                                .setBinding(value -> {
                                    this.vanillaOpts.guiScale().set(value);
                                    Minecraft.getInstance().resizeDisplay();
                                }, this.vanillaOpts.guiScale()::get)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:general.fullscreen"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.fullscreen"))
                                .setTooltip(Component.translatable("sodium.options.fullscreen.tooltip"))
                                .setDefaultValue(false)
                                .setBinding(value -> {
                                    this.vanillaOpts.fullscreen().set(value);

                                    if (this.window.isFullscreen() != this.vanillaOpts.fullscreen().get()) {
                                        this.window.toggleFullScreen();

                                        // The client might not be able to enter full-screen mode
                                        this.vanillaOpts.fullscreen().set(this.window.isFullscreen());
                                    }
                                }, this.vanillaOpts.fullscreen()::get)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:general.fullscreen_resolution"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.fullscreen.resolution"))
                                .setTooltip(Component.translatable("sodium.options.fullscreen_resolution.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.resolution())
                                // the max value of 1 when the monitor is not available prevents an exception from being thrown
                                .setRange(0, this.monitor != null ? this.monitor.getModeCount() : 1, 1)
                                .setDefaultValue(0)
                                .setBinding(value -> {
                                    if (this.monitor != null) {
                                        this.window.setPreferredFullscreenVideoMode(0 == value ? Optional.empty() : Optional.of(this.monitor.getMode(value - 1)));
                                    }
                                }, () -> {
                                    if (this.monitor == null) {
                                        return 0;
                                    } else {
                                        Optional<VideoMode> optional = this.window.getPreferredFullscreenVideoMode();
                                        return optional.map((videoMode) -> this.monitor.getVideoModeIndex(videoMode) + 1).orElse(0);
                                    }
                                })
                                .setEnabledProvider(
                                        (state) -> {
                                            if (this.monitor == null || this.monitor.getModeCount() <= 0) {
                                                return false;
                                            }
                                            var os = OsUtils.getOs();
                                            return (os == OsUtils.OperatingSystem.WIN || os == OsUtils.OperatingSystem.MAC) &&
                                                    state.readBooleanOption(Identifier.parse("sodium:general.fullscreen"));
                                        },
                                        Identifier.parse("sodium:general.fullscreen"))
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:general.vsync"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.vsync"))
                                .setTooltip(Component.translatable("sodium.options.v_sync.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.enableVsync()::set, this.vanillaOpts.enableVsync()::get)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:general.framerate_limit"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.framerateLimit"))
                                .setTooltip(Component.translatable("sodium.options.fps_limit.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.fpsLimit())
                                .setRange(10, 260, 10)
                                .setDefaultValue(60)
                                .setBinding(this.vanillaOpts.framerateLimit()::set, this.vanillaOpts.framerateLimit()::get)
                )
        );
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:general.view_bobbing"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.viewBobbing"))
                                .setTooltip(Component.translatable("sodium.options.view_bobbing.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.bobView()::set, this.vanillaOpts.bobView()::get)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:general.attack_indicator"), AttackIndicatorStatus.class)
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.attackIndicator"))
                                .setTooltip(Component.translatable("sodium.options.attack_indicator.tooltip"))
                                .setDefaultValue(AttackIndicatorStatus.CROSSHAIR)
                                .setElementNameProvider(AttackIndicatorStatus::caption)
                                .setBinding(this.vanillaOpts.attackIndicator()::set, this.vanillaOpts.attackIndicator()::get)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:general.autosave_indicator"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.autosaveIndicator"))
                                .setTooltip(Component.translatable("sodium.options.autosave_indicator.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.showAutosaveIndicator()::set, this.vanillaOpts.showAutosaveIndicator()::get)
                )
        );
        return generalPage;
    }

    private OptionPageBuilder buildQualityPage(ConfigBuilder builder) {
        var qualityPage = builder.createOptionPage().setName(Component.translatable("sodium.options.pages.quality"));

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:quality.graphics"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.improvedTransparency"))
                                .setTooltip(Component.translatable("options.improvedTransparency.tooltip"))
                                .setDefaultValue(false)
                                .setBinding(this.vanillaOpts.improvedTransparency()::set, this.vanillaOpts.improvedTransparency()::get)
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.clouds"), CloudStatus.class)
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.renderClouds"))
                                .setTooltip(Component.translatable("sodium.options.clouds_quality.tooltip"))
                                .setElementNameProvider(EnumOptionBuilder.nameProviderFrom(
                                        Component.translatable("options.off"),
                                        Component.translatable("options.clouds.fast"),
                                        Component.translatable("options.clouds.fancy")))
                                .setDefaultValue(CloudStatus.FANCY)
                                .setBinding((value) -> {
                                    this.vanillaOpts.cloudStatus().set(value);

                                    if (Minecraft.useShaderTransparency()) {
                                        RenderTarget framebuffer = Minecraft.getInstance().levelRenderer.getCloudsTarget();
                                        if (framebuffer != null) {
                                            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorTexture(), 0xFFFFFFFF, framebuffer.getDepthTexture(), 1.0f);
                                        }
                                    }
                                }, () -> this.vanillaOpts.cloudStatus().get())
                                .setImpact(OptionImpact.LOW)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.render_cloud_distance"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.renderCloudsDistance"))
                                .setTooltip(Component.translatable("sodium.options.clouds_distance.tooltip"))
                                .setRange(2, 128, 2)
                                .setDefaultValue(128)
                                .setBinding((value) -> {
                                    this.vanillaOpts.cloudRange().set(value);

                                    Minecraft.getInstance().levelRenderer.getCloudRenderer().markForRebuild();
                                }, () -> this.vanillaOpts.cloudRange().get())
                                .setImpact(OptionImpact.LOW)
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("options.chunks"))
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.weather"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.weatherRadius"))
                                .setTooltip(Component.translatable("options.weatherRadius.tooltip"))
                                .setDefaultValue(10)
                                .setRange(new Range(3, 10, 1))
                                .setValueFormatter(ControlValueFormatterImpls.number())
                                .setBinding(this.vanillaOpts.weatherRadius()::set, this.vanillaOpts.weatherRadius()::get)
                                .setImpact(OptionImpact.LOW)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:quality.leaves"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.cutoutLeaves"))
                                .setTooltip(Component.translatable("options.cutoutLeaves.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.cutoutLeaves()::set, this.vanillaOpts.cutoutLeaves()::get)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.particles"), ParticleStatus.class)
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.particles"))
                                .setTooltip(Component.translatable("sodium.options.particle_quality.tooltip"))
                                .setElementNameProvider(EnumOptionBuilder.nameProviderFrom(
                                        Component.translatable("options.particles.all"),
                                        Component.translatable("options.particles.decreased"),
                                        Component.translatable("options.particles.minimal")
                                ))
                                .setDefaultValue(ParticleStatus.ALL)
                                .setBinding(this.vanillaOpts.particles()::set, this.vanillaOpts.particles()::get)
                                .setImpact(OptionImpact.MEDIUM)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:quality.ao"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.ao"))
                                .setTooltip(Component.translatable("sodium.options.smooth_lighting.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.ambientOcclusion()::set, this.vanillaOpts.ambientOcclusion()::get)
                                .setImpact(OptionImpact.LOW)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.biome_blend"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.biomeBlendRadius"))
                                .setValueFormatter(ControlValueFormatterImpls.biomeBlend())
                                .setTooltip(Component.translatable("sodium.options.biome_blend.tooltip"))
                                .setRange(0, 7, 1)
                                .setDefaultValue(2)
                                .setBinding(this.vanillaOpts.biomeBlendRadius()::set, this.vanillaOpts.biomeBlendRadius()::get)
                                .setImpact(OptionImpact.LOW)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.entity_distance"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.entityDistanceScaling"))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setTooltip(Component.translatable("sodium.options.entity_distance.tooltip"))
                                .setRange(50, 500, 25)
                                .setDefaultValue(100)
                                .setBinding((value) -> this.vanillaOpts.entityDistanceScaling().set(value / 100.0), () -> Math.round(this.vanillaOpts.entityDistanceScaling().get().floatValue() * 100.0F))
                                .setImpact(OptionImpact.HIGH)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:quality.entity_shadows"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.entityShadows"))
                                .setTooltip(Component.translatable("sodium.options.entity_shadows.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.entityShadows()::set, this.vanillaOpts.entityShadows()::get)
                                .setImpact(OptionImpact.MEDIUM)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:quality.vignette"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.vignette"))
                                .setTooltip(Component.translatable("options.vignette.tooltip"))
                                .setDefaultValue(true)
                                .setBinding(this.vanillaOpts.vignette()::set, this.vanillaOpts.vignette()::get)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.fade_time"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.chunkFade"))
                                .setTooltip(Component.translatable("options.chunkFade.tooltip"))
                                .setDefaultValue(750)
                                .setValueFormatter(ControlValueFormatterImpls.chunkFade())
                                .setRange(new Range(0, 2000, 50))
                                .setBinding(fade -> this.vanillaOpts.chunkSectionFadeInTime().set((double) fade / 1000.0), () -> (int) (this.vanillaOpts.chunkSectionFadeInTime().get() * 1000.0))
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.mipmap_levels"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.mipmapLevels"))
                                .setValueFormatter(ControlValueFormatterImpls.multiplier())
                                .setTooltip(Component.translatable("sodium.options.mipmap_levels.tooltip"))
                                .setRange(0, 4, 1)
                                .setDefaultValue(4)
                                .setBinding(this.vanillaOpts.mipmapLevels()::set, this.vanillaOpts.mipmapLevels()::get)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.filtering_mode"), TextureFilteringMethod.class)
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.textureFiltering"))
                                .setTooltip(i -> Component.translatable("options.textureFiltering." + i.name().toLowerCase(Locale.ROOT) + ".tooltip"))
                                .setElementNameProvider(name -> {
                                    return Component.translatable("options.textureFiltering." + name.name().toLowerCase(Locale.ROOT));
                                })
                                .setDefaultValue(TextureFilteringMethod.RGSS)
                                .setBinding(this.vanillaOpts.textureFiltering()::set, this.vanillaOpts.textureFiltering()::get)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:quality.anisotropy_bit"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.maxAnisotropy"))
                                .setRange(new Range(0, 3, 1))
                                .setTooltip(Component.translatable("options.maxAnisotropy.tooltip"))
                                .setDefaultValue(0)
                                .setValueFormatter(ControlValueFormatterImpls.anisotropyBit())
                                .setBinding(this.vanillaOpts.maxAnisotropyBit()::set, this.vanillaOpts.maxAnisotropyBit()::get)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setEnabledProvider(i -> {
                                    return i.readEnumOption(Identifier.parse("sodium:quality.filtering_mode"), TextureFilteringMethod.class) == TextureFilteringMethod.ANISOTROPIC;
                                }, Identifier.parse("sodium:quality.filtering_mode"))
                )
        );
        return qualityPage;
    }

    private OptionPageBuilder buildPerformancePage(ConfigBuilder builder) {
        var performancePage = builder.createOptionPage().setName(Component.translatable("sodium.options.pages.performance"));

        performancePage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:performance.chunk_update_threads"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.chunk_update_threads.name"))
                                .setValueFormatter(ControlValueFormatterImpls.quantityOrDisabled(
                                        (v) -> Component.translatable("sodium.options.chunk_update_threads.value", v),
                                        Component.translatable("sodium.options.default")
                                ))
                                .setTooltip(Component.translatable("sodium.options.chunk_update_threads.tooltip"))
                                .setRange(0, Runtime.getRuntime().availableProcessors(), 1)
                                .setDefaultValue(DEFAULTS.performance.chunkBuilderThreads)
                                .setBinding(value -> this.sodiumOpts.performance.chunkBuilderThreads = value, () -> this.sodiumOpts.performance.chunkBuilderThreads)
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:performance.always_defer_chunk_updates"), DeferMode.class)
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.defer_chunk_updates.name"))
                                .setTooltip(Component.translatable("sodium.options.defer_chunk_updates.tooltip"))
                                .setDefaultValue(DEFAULTS.performance.chunkBuildDeferMode)
                                .setBinding(value -> this.sodiumOpts.performance.chunkBuildDeferMode = value, () -> this.sodiumOpts.performance.chunkBuildDeferMode)
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                )
        );

        performancePage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:performance.use_block_face_culling"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.use_block_face_culling.name"))
                                .setTooltip(Component.translatable("sodium.options.use_block_face_culling.tooltip"))
                                .setDefaultValue(DEFAULTS.performance.useBlockFaceCulling)
                                .setBinding(value -> this.sodiumOpts.performance.useBlockFaceCulling = value, () -> this.sodiumOpts.performance.useBlockFaceCulling)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:performance.use_fog_occlusion"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.use_fog_occlusion.name"))
                                .setTooltip(Component.translatable("sodium.options.use_fog_occlusion.tooltip"))
                                .setDefaultValue(DEFAULTS.performance.useFogOcclusion)
                                .setBinding(value -> this.sodiumOpts.performance.useFogOcclusion = value, () -> this.sodiumOpts.performance.useFogOcclusion)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:performance.use_entity_culling"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.use_entity_culling.name"))
                                .setTooltip(Component.translatable("sodium.options.use_entity_culling.tooltip"))
                                .setDefaultValue(DEFAULTS.performance.useEntityCulling)
                                .setBinding(value -> this.sodiumOpts.performance.useEntityCulling = value, () -> this.sodiumOpts.performance.useEntityCulling)
                                .setImpact(OptionImpact.MEDIUM)
                )
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:performance.animate_only_visible_textures"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.animate_only_visible_textures.name"))
                                .setTooltip(Component.translatable("sodium.options.animate_only_visible_textures.tooltip"))
                                .setDefaultValue(DEFAULTS.performance.animateOnlyVisibleTextures)
                                .setBinding(value -> this.sodiumOpts.performance.animateOnlyVisibleTextures = value, () -> this.sodiumOpts.performance.animateOnlyVisibleTextures)
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                )
                .addOption(
                        this.buildNoErrorContextOption(builder)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:performance.inactivity_fps_limit"), InactivityFpsLimit.class)
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.inactivityFpsLimit"))
                                .setElementNameProvider(InactivityFpsLimit::caption)
                                .setTooltip((state) -> state == InactivityFpsLimit.AFK ?
                                        Component.translatable("options.inactivityFpsLimit.afk.tooltip") :
                                        Component.translatable("options.inactivityFpsLimit.minimized.tooltip"))
                                .setDefaultValue(InactivityFpsLimit.AFK)
                                .setBinding(this.vanillaOpts.inactivityFpsLimit()::set, this.vanillaOpts.inactivityFpsLimit()::get)
                )
        );

        if (PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment()) {
            performancePage.addOptionGroup(builder.createOptionGroup()
                    .addOption(
                            builder.createEnumOption(Identifier.parse("sodium:performance.quad_splitting"), QuadSplittingMode.class)
                                    .setStorageHandler(this.sodiumStorage)
                                    .setName(Component.translatable("sodium.options.quad_splitting.name"))
                                    .setTooltip(Component.translatable("sodium.options.quad_splitting.tooltip"))
                                    .setImpact(OptionImpact.MEDIUM)
                                    .setDefaultValue(DEFAULTS.performance.quadSplittingMode)
                                    .setBinding(value -> this.sodiumOpts.performance.quadSplittingMode = value, () -> this.sodiumOpts.performance.quadSplittingMode)
                                    .setEnabled(SodiumClientMod.options().debug.terrainSortingEnabled)
                                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                    )
            );
        }
        return performancePage;
    }

    private OptionBuilder buildNoErrorContextOption(ConfigBuilder builder) {
        return builder.createBooleanOption(Identifier.parse("sodium:performance.use_no_error_context"))
                .setStorageHandler(this.sodiumStorage)
                .setName(Component.translatable("sodium.options.use_no_error_context.name"))
                .setTooltip(Component.translatable("sodium.options.use_no_error_context.tooltip"))
                .setDefaultValue(DEFAULTS.performance.useNoErrorGLContext)
                .setBinding(value -> this.sodiumOpts.performance.useNoErrorGLContext = value, () -> this.sodiumOpts.performance.useNoErrorGLContext)
                .setEnabledProvider((state) -> {
                    GLCapabilities capabilities = GL.getCapabilities();
                    return (capabilities.OpenGL46 || capabilities.GL_KHR_no_error)
                            && !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
                })
                .setImpact(OptionImpact.LOW)
                .setFlags(OptionFlag.REQUIRES_GAME_RESTART);
    }

    private OptionPageBuilder buildAdvancedPage(ConfigBuilder builder) {
        var advancedPage = builder.createOptionPage().setName(Component.translatable("sodium.options.pages.advanced"));

        boolean isPersistentMappingSupported = MappedStagingBuffer.isSupported(RenderDevice.INSTANCE);

        advancedPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createBooleanOption(Identifier.parse("sodium:advanced.use_persistent_mapping"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.use_persistent_mapping.name"))
                                .setTooltip(Component.translatable("sodium.options.use_persistent_mapping.tooltip"))
                                .setDefaultValue(DEFAULTS.advanced.useAdvancedStagingBuffers)
                                .setBinding(value -> this.sodiumOpts.advanced.useAdvancedStagingBuffers = value, () -> this.sodiumOpts.advanced.useAdvancedStagingBuffers)
                                .setEnabled(isPersistentMappingSupported)
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
        );

        advancedPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:advanced.cpu_render_ahead_limit"))
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.cpu_render_ahead_limit.name"))
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("sodium.options.cpu_render_ahead_limit.value"))
                                .setTooltip(Component.translatable("sodium.options.cpu_render_ahead_limit.tooltip"))
                                .setRange(0, 9, 1)
                                .setDefaultValue(DEFAULTS.advanced.cpuRenderAheadLimit)
                                .setBinding(value -> this.sodiumOpts.advanced.cpuRenderAheadLimit = value, () -> this.sodiumOpts.advanced.cpuRenderAheadLimit)
                )
        );
        return advancedPage;
    }

}