package net.caffeinemc.mods.sodium.client.gui.screen;

import net.caffeinemc.mods.sodium.client.checks.ResourcePackScanner;
import net.caffeinemc.mods.sodium.client.checks.ResourcePackScanner.ResourcePackProblem;
import net.caffeinemc.mods.sodium.client.checks.ResourcePackScanner.Severity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourcePackIssuesScreen extends Screen {
    private static final String RESOURCE_PACK_DOCUMENTATION =
            "https://github.com/CaffeineMC/sodium/wiki/Resource-Packs";

    private static final Component TITLE = Component.translatable("sodium.compatibility_issues.title");
    private static final Component INTRO = Component.translatable("sodium.compatibility_issues.intro");

    private final Screen parent;

    private HeaderAndFooterLayout layout;
    private @Nullable ScrollableLayout bodyScroll;
    private List<ResourcePackProblem> shownProblems = List.of();

    public ResourcePackIssuesScreen(Screen parent) {
        super(TITLE);

        this.parent = parent;
    }

    @Override
    protected void init() {
        this.shownProblems = ResourcePackScanner.getCurrentProblems();
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(TITLE, this.font);

        var body = LinearLayout.vertical().spacing(8);
        body.defaultCellSetting().alignHorizontallyCenter();

        int contentWidth = Math.max(200, Math.min(360, this.width - 40));

        body.addChild(FocusableTextWidget.builder(INTRO, this.font)
                .maxWidth(contentWidth)
                .alwaysShowBorder(false)
                .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
                .build());

        if (this.shownProblems.isEmpty()) {
            body.addChild(FocusableTextWidget.builder(
                            Component.translatable("sodium.compatibility_issues.none"), this.font)
                    .maxWidth(contentWidth)
                    .alwaysShowBorder(false)
                    .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
                    .build());
        } else {
            for (var problem : this.shownProblems) {
                body.addChild(this.createProblemWidget(problem, contentWidth));
            }
        }

        this.bodyScroll = new ScrollableLayout(this.minecraft, body, this.layout.getContentHeight());
        this.bodyScroll.setMinWidth(contentWidth);
        this.layout.addToContents(this.bodyScroll);

        var footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(
                        Component.translatable("sodium.compatibility_issues.resource_packs"),
                        button -> this.openResourcePackScreen())
                .width(110)
                .build());
        footer.addChild(Button.builder(
                        Component.translatable("sodium.compatibility_issues.learn_more"),
                        ConfirmLinkScreen.confirmLink(this, RESOURCE_PACK_DOCUMENTATION))
                .width(90)
                .build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .width(70)
                .build());

        this.layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        this.repositionElements();
    }

    private FocusableTextWidget createProblemWidget(ResourcePackProblem problem, int width) {
        var severityKey = problem.severity() == Severity.SEVERE
                ? "sodium.compatibility_issues.problem.severe"
                : "sodium.compatibility_issues.problem.warn";
        var descriptionKey = problem.severity() == Severity.SEVERE
                ? "sodium.compatibility_issues.description.severe"
                : "sodium.compatibility_issues.description.warn";
        var resolutionKey = problem.serverPack()
                ? "sodium.compatibility_issues.resolution.server"
                : "sodium.compatibility_issues.resolution.local";
        var color = problem.severity() == Severity.SEVERE ? ChatFormatting.RED : ChatFormatting.GOLD;

        MutableComponent message = Component.translatable(severityKey, problem.name())
                .withStyle(color, ChatFormatting.BOLD)
                .append("\n")
                .append(Component.translatable(descriptionKey).withStyle(ChatFormatting.WHITE))
                .append("\n")
                .append(Component.translatable(
                        "sodium.compatibility_issues.files", String.join(", ", problem.resources()))
                        .withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.translatable(resolutionKey).withStyle(ChatFormatting.WHITE));

        return FocusableTextWidget.builder(message, this.font)
                .maxWidth(width)
                .alwaysShowBorder(true)
                .backgroundFill(FocusableTextWidget.BackgroundFill.ALWAYS)
                .build();
    }

    private void openResourcePackScreen() {
        this.minecraft.gui.setScreen(new PackSelectionScreen(
                this.minecraft.getResourcePackRepository(),
                this::applyResourcePacks,
                this.minecraft.getResourcePackDirectory(),
                Component.translatable("resourcePack.title")));
    }

    private void applyResourcePacks(PackRepository repository) {
        this.minecraft.options.updateResourcePacks(repository);
        this.minecraft.gui.setScreen(this);
    }

    @Override
    public void tick() {
        if (!this.shownProblems.equals(ResourcePackScanner.getCurrentProblems())) {
            this.rebuildWidgets();
        }
    }

    @Override
    protected void repositionElements() {
        if (this.bodyScroll == null) {
            return;
        }

        this.bodyScroll.arrangeElements();
        this.bodyScroll.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), INTRO);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
