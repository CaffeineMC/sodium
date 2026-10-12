package net.caffeinemc.mods.sodium.client.gui;

import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.config.vanilla.IntProjection;
import net.caffeinemc.mods.sodium.client.config.vanilla.VanillaOption;
import net.caffeinemc.mods.sodium.client.config.vanilla.VanillaOptions;
import net.caffeinemc.mods.sodium.client.gui.options.FullscreenMode;
import net.caffeinemc.mods.sodium.client.gui.options.Toggle;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatterImpls;
import net.caffeinemc.mods.sodium.client.render.chunk.DeferMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
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

public class SodiumConfigBuilder implements ConfigEntryPoint {
    private static final Identifier SODIUM_ICON = Identifier.fromNamespaceAndPath("sodium", "textures/gui/config-icon.png");
    private static final SodiumOptions DEFAULTS = SodiumOptions.defaults();

    private final Options vanillaOpts;
    private final StorageEventHandler vanillaStorage;
    private final SodiumOptions sodiumOpts;
    private final StorageEventHandler sodiumStorage;

    private final @Nullable Window window;

    public SodiumConfigBuilder() {
        var minecraft = Minecraft.getInstance();
        this.window = minecraft.getWindow();

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

    private Monitor getMonitor() {
        if (this.window == null) {
            return null;
        }
        return this.window.findBestMonitor();
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
        var vanilla = new VanillaOptions(builder, this.vanillaStorage);

        createModOptionsBuilder(builder)
                .setColorTheme(builder.createColorTheme().setFullThemeRGB(
                        Colors.THEME, Colors.THEME_LIGHTER, Colors.THEME_DARKER))
                .addPage(this.buildGeneralPage(builder, vanilla))
                .addPage(this.buildQualityPage(builder, vanilla))
                .addPage(this.buildPerformancePage(builder, vanilla));
    }

    private OptionPageBuilder buildGeneralPage(ConfigBuilder builder, VanillaOptions vanilla) {
        var generalPage = builder.createOptionPage().setName(Component.translatable("sodium.options.pages.general"));
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:general.render_distance"), this.vanillaOpts.renderDistance())
                                .setTooltip(Component.translatable("sodium.options.view_distance.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("options.chunks"))
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:general.simulation_distance"), this.vanillaOpts.simulationDistance())
                                .setTooltip(Component.translatable("sodium.options.simulation_distance.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("options.chunks"))
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:general.gamma"), this.vanillaOpts.gamma(), IntProjection.scaled(100))
                                .setTooltip(Component.translatable("sodium.options.brightness.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.brightness())
                )
        );

        var fullscreen = new VanillaOption<>(this.vanillaOpts.fullscreen());
        var exclusiveFullscreen = new VanillaOption<>(this.vanillaOpts.exclusiveFullscreen());
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:general.gui_scale"), this.vanillaOpts.guiScale())
                                .setTooltip(Component.translatable("sodium.options.gui_scale.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.guiScale())
                                .setValidatorProvider((state) -> {
                                    var savedValue = state.readIntOption(Identifier.parse("sodium:general.gui_scale"));
                                    var realMax = this.window.calculateScale(0, Minecraft.getInstance().isEnforceUnicode());
                                    var presentationMax = Math.max(savedValue, realMax);
                                    return new GUIScaleRange(presentationMax);
                                }, ConfigState.UPDATE_ON_REBUILD, ConfigState.UPDATE_ON_APPLY)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:general.fullscreen_mode"), FullscreenMode.class)
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("sodium.options.fullscreen_mode.name"))
                                .setTooltip(Component.translatable("sodium.options.fullscreen_mode.tooltip"))
                                .setElementNameProvider(mode -> switch (mode) {
                                    case OFF -> Component.translatable("sodium.options.fullscreen_mode.off");
                                    case EXCLUSIVE ->
                                            Component.translatable("sodium.options.fullscreen_mode.exclusive");
                                    case BORDERLESS ->
                                            Component.translatable("sodium.options.fullscreen_mode.borderless");
                                })
                                .setDefaultValue(FullscreenMode.of(fullscreen.defaultValue(), exclusiveFullscreen.defaultValue()))
                                .setImpact(OptionImpact.HIGH)
                                .setBinding(
                                        // modifies fullscreen and exclusive fullscreen together since they are interdependent in Vanilla's implementation
                                        value -> {
                                            switch (value) {
                                                case OFF -> fullscreen.set(false);
                                                case EXCLUSIVE -> {
                                                    fullscreen.set(true);
                                                    exclusiveFullscreen.set(true);
                                                }
                                                case BORDERLESS -> {
                                                    fullscreen.set(true);
                                                    exclusiveFullscreen.set(false);
                                                }
                                            }
                                        },
                                        () -> FullscreenMode.of(fullscreen.get(), exclusiveFullscreen.get()))
                                .setApplyHook((_) -> {
                                })
                )
                .addOption(
                        builder.createIntegerOption(Identifier.parse("sodium:general.fullscreen_resolution"))
                                .setStorageHandler(this.vanillaStorage)
                                .setName(Component.translatable("options.fullscreen.resolution"))
                                .setTooltip(Component.translatable("sodium.options.fullscreen_resolution.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.resolution())
                                // the max value of 1 when the monitor is not available prevents an exception from being thrown
                                .setValidator(new FullscreenResolutionRange())
                                .setDefaultValue(0)
                                .setBinding(value -> {
                                    var monitor = this.getMonitor();
                                    if (monitor != null) {
                                        this.window.setPreferredFullscreenVideoMode(0 == value ? Optional.empty() : Optional.of(monitor.mode(value - 1)));
                                    }
                                }, () -> {
                                    var monitor = this.getMonitor();
                                    if (monitor == null) {
                                        return 0;
                                    } else {
                                        Optional<VideoMode> optional = this.window.getPreferredFullscreenVideoMode();
                                        return optional.map((videoMode) -> monitor.indexOfMode(videoMode) + 1).orElse(0);
                                    }
                                })
                                .setEnabledProvider(
                                        (state) -> {
                                            var monitor = this.getMonitor();
                                            if (monitor == null || monitor.modeCount() <= 0) {
                                                return false;
                                            }
                                            var fullscreenMode = state.readEnumOption(Identifier.parse("sodium:general.fullscreen_mode"), FullscreenMode.class);
                                            return fullscreenMode == FullscreenMode.EXCLUSIVE;
                                        },
                                        Identifier.parse("sodium:general.fullscreen_mode"))
                                .setFlags(OptionFlag.REQUIRES_VIDEOMODE_RELOAD)
                )
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:general.vsync"), this.vanillaOpts.enableVsync())
                                .setTooltip(Component.translatable("sodium.options.v_sync.tooltip"))
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:general.framerate_limit"), this.vanillaOpts.framerateLimit())
                                .setTooltip(Component.translatable("sodium.options.fps_limit.tooltip"))
                                .setValueFormatter(ControlValueFormatterImpls.fpsLimit())
                )
        );
        generalPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createEnumOption(Identifier.parse("sodium:general.attack_indicator"), AttackIndicatorStatus.class, this.vanillaOpts.attackIndicator())
                                .setTooltip(Component.translatable("sodium.options.attack_indicator.tooltip"))
                )
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:general.autosave_indicator"), this.vanillaOpts.showAutosaveIndicator())
                                .setTooltip(Component.translatable("sodium.options.autosave_indicator.tooltip"))
                )
        );

        var platformGroup = builder.createOptionGroup().addOption(
                vanilla.createEnumOption(Identifier.fromNamespaceAndPath("sodium", "general.graphics_api"), PreferredGraphicsApi.class, this.vanillaOpts.preferredGraphicsBackend())
                        .setTooltip(i -> {
                            if (i == PreferredGraphicsApi.VULKAN) {
                                return Component.translatable("options.graphicsApi.tooltip.vulkan");
                            } else {
                                return Component.translatable("options.graphicsApi.tooltip");
                            }
                        })
                        .setElementNameProvider(EnumOptionBuilder.nameProviderFrom(
                                Component.translatable("options.graphicsApi.default"),
                                Component.translatable("options.graphicsApi.opengl"),
                                Component.literal("Prefer Vulkan")))
                        .setFlags(OptionFlag.REQUIRES_GAME_RESTART));

        if (MacosUtil.IS_MACOS) {
            platformGroup.addOption(
                    vanilla.createBooleanOption(Identifier.parse("sodium:general.mac_fullscreen_menu_visibility"), this.vanillaOpts.macFullscreenMenuVisibility())
            );
        }
        generalPage.addOptionGroup(platformGroup);

        return generalPage;
    }

    private OptionPageBuilder buildQualityPage(ConfigBuilder builder, VanillaOptions vanilla) {
        var qualityPage = builder.createOptionPage().setName(Component.translatable("sodium.options.pages.quality"));

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:quality.graphics"), this.vanillaOpts.improvedTransparency())
                                .setImpact(OptionImpact.HIGH)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createEnumOption(Identifier.parse("sodium:quality.clouds"), CloudStatus.class, this.vanillaOpts.cloudStatus())
                                .setTooltip(Component.translatable("sodium.options.clouds_quality.tooltip"))
                                .setImpact(OptionImpact.LOW)
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.render_cloud_distance"), this.vanillaOpts.cloudRange())
                                .setTooltip(Component.translatable("sodium.options.clouds_distance.tooltip"))
                                .setApplyHook((_) -> Minecraft.getInstance().levelRenderer.cloudRenderer().markForRebuild())
                                .setImpact(OptionImpact.LOW)
                                .setValueFormatter(ControlValueFormatterImpls.translateVariable("options.chunks"))
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.weather"), this.vanillaOpts.weatherRadius())
                                .setValueFormatter(ControlValueFormatterImpls.number())
                                .setImpact(OptionImpact.LOW)
                )
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:quality.leaves"), this.vanillaOpts.cutoutLeaves())
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        vanilla.createEnumOption(Identifier.parse("sodium:quality.particles"), ParticleStatus.class, this.vanillaOpts.particles())
                                .setTooltip(Component.translatable("sodium.options.particle_quality.tooltip"))
                                .setImpact(OptionImpact.MEDIUM)
                )
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:quality.ao"), this.vanillaOpts.ambientOcclusion())
                                .setTooltip(Component.translatable("sodium.options.smooth_lighting.tooltip"))
                                .setImpact(OptionImpact.LOW)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.biome_blend"), this.vanillaOpts.biomeBlendRadius())
                                .setValueFormatter(ControlValueFormatterImpls.biomeBlend())
                                .setTooltip(Component.translatable("sodium.options.biome_blend.tooltip"))
                                .setImpact(OptionImpact.LOW)
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.entity_distance"), this.vanillaOpts.entityDistanceScaling(), IntProjection.scaled(100))
                                .setValueFormatter(ControlValueFormatterImpls.percentage())
                                .setTooltip(Component.translatable("sodium.options.entity_distance.tooltip"))
                                .setImpact(OptionImpact.HIGH)
                )
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:quality.entity_shadows"), this.vanillaOpts.entityShadows())
                                .setTooltip(Component.translatable("sodium.options.entity_shadows.tooltip"))
                                .setImpact(OptionImpact.MEDIUM)
                )
                .addOption(
                        vanilla.createBooleanOption(Identifier.parse("sodium:quality.vignette"), this.vanillaOpts.vignette())
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.fade_time"), this.vanillaOpts.chunkSectionFadeInTime(), IntProjection.scaled(1000))
                                .setValueFormatter(ControlValueFormatterImpls.chunkFade())
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.mipmap_levels"), this.vanillaOpts.mipmapLevels())
                                .setValueFormatter(ControlValueFormatterImpls.multiplier())
                                .setTooltip(Component.translatable("sodium.options.mipmap_levels.tooltip"))
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        vanilla.createEnumOption(Identifier.parse("sodium:quality.filtering_mode"), TextureFilteringMethod.class, this.vanillaOpts.textureFiltering())
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                )
                .addOption(
                        vanilla.createIntegerOption(Identifier.parse("sodium:quality.anisotropy_bit"), this.vanillaOpts.maxAnisotropyBit())
                                .setValueFormatter(ControlValueFormatterImpls.anisotropyBit())
                                .setImpact(OptionImpact.MEDIUM)
                                .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                                .setEnabledProvider(i -> i.readEnumOption(Identifier.parse("sodium:quality.filtering_mode"), TextureFilteringMethod.class) == TextureFilteringMethod.ANISOTROPIC, Identifier.parse("sodium:quality.filtering_mode"))
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.pixel_filtering_mode"), FilterMode.class)
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.pixel_filtering_mode.name"))
                                .setTooltip(Component.translatable("sodium.options.pixel_filtering_mode.tooltip"))
                                .setElementNameProvider(filterMode ->
                                        Component.translatable("sodium.options.pixel_filtering_mode." + filterMode.name().toLowerCase(Locale.ROOT))
                                )
                                .setDefaultValue(FilterMode.NEAREST)
                                .setBinding(filterMode -> {
                                    this.sodiumOpts.quality.pixelFilteringMode = filterMode;
                                    Minecraft.getInstance().levelExtractor.resetSampler();
                                }, () -> this.sodiumOpts.quality.pixelFilteringMode)
                                .setImpact(OptionImpact.MEDIUM)
                )
        );

        qualityPage.addOptionGroup(builder.createOptionGroup()
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.hidden_fluid_culling"), Toggle.class)
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.hidden_fluid_culling.name"))
                                .setTooltip(Component.translatable("sodium.options.hidden_fluid_culling.tooltip"))
                                .setElementNameProvider(EnumOptionBuilder.nameProviderFrom(
                                        Component.translatable("sodium.options.hidden_fluid_culling.default"),
                                        Component.translatable("sodium.options.hidden_fluid_culling.optimized")))
                                .setImpact(OptionImpact.MEDIUM)
                                .setDefaultValue(Toggle.fromBoolean(DEFAULTS.quality.hiddenFluidCulling))
                                .setBinding(value -> this.sodiumOpts.quality.hiddenFluidCulling = value.toBoolean(), () -> Toggle.fromBoolean(this.sodiumOpts.quality.hiddenFluidCulling))
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.improved_fluid_shaping"), Toggle.class)
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.improved_fluid_shaping.name"))
                                .setTooltip(Component.translatable("sodium.options.improved_fluid_shaping.tooltip"))
                                .setElementNameProvider(EnumOptionBuilder.nameProviderFrom(
                                        Component.translatable("sodium.options.improved_fluid_shaping.default"),
                                        Component.translatable("sodium.options.improved_fluid_shaping.alternative")))
                                .setDefaultValue(Toggle.fromBoolean(DEFAULTS.quality.improvedFluidShaping))
                                .setBinding(value -> this.sodiumOpts.quality.improvedFluidShaping = value.toBoolean(), () -> Toggle.fromBoolean(this.sodiumOpts.quality.improvedFluidShaping))
                                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                        builder.createEnumOption(Identifier.parse("sodium:quality.closest_point_entity_sort"), Toggle.class)
                                .setStorageHandler(this.sodiumStorage)
                                .setName(Component.translatable("sodium.options.closest_point_entity_sort.name"))
                                .setTooltip(Component.translatable("sodium.options.closest_point_entity_sort.tooltip"))
                                .setElementNameProvider(EnumOptionBuilder.nameProviderFrom(
                                        Component.translatable("sodium.options.closest_point_entity_sort.default"),
                                        Component.translatable("sodium.options.closest_point_entity_sort.enhanced")))
                                .setImpact(OptionImpact.MEDIUM)
                                .setDefaultValue(Toggle.fromBoolean(DEFAULTS.quality.useClosestPointEntitySort))
                                .setBinding(value -> this.sodiumOpts.quality.useClosestPointEntitySort = value.toBoolean(), () -> Toggle.fromBoolean(this.sodiumOpts.quality.useClosestPointEntitySort))
                )
        );
        return qualityPage;
    }

    private OptionPageBuilder buildPerformancePage(ConfigBuilder builder, VanillaOptions vanilla) {
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
                        vanilla.createEnumOption(Identifier.parse("sodium:performance.inactivity_fps_limit"), InactivityFpsLimit.class, this.vanillaOpts.inactivityFpsLimit())
                )
        );

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
                    if (!RenderSystem.getDevice().getDeviceInfo().backendName().contains("OpenGL")) return false;
                    GLCapabilities capabilities = GL.getCapabilities();
                    return (capabilities.OpenGL46 || capabilities.GL_KHR_no_error)
                            && !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
                })
                .setImpact(OptionImpact.LOW)
                .setFlags(OptionFlag.REQUIRES_GAME_RESTART);
    }

}