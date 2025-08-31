package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.network.chat.Component;

public class DonationButtonWidget {
    private static final int DONATE_BUTTON_WIDTH = 100;
    private static final int CLOSE_BUTTON_MARGIN = 3;

    private final FlatButtonWidget hideDonateButton;
    private final FlatButtonWidget donateButtonText;
    private boolean donateButtonEnabled;
    
    public DonationButtonWidget(VideoSettingsScreen parent, int screenWidth, Runnable openDonationPage, Runnable hideDonationButton) {
        int y = 0; // Layout.INNER_MARGIN;
        this.hideDonateButton = new FlatButtonWidget(new Dim2i(screenWidth - Layout.BUTTON_SHORT - Layout.INNER_MARGIN, y, Layout.BUTTON_SHORT, Layout.BUTTON_SHORT), Component.literal("x"), hideDonationButton, true, false);
        this.donateButtonText = new FlatButtonWidget(new Dim2i(this.hideDonateButton.getX() - CLOSE_BUTTON_MARGIN - DONATE_BUTTON_WIDTH, y, DONATE_BUTTON_WIDTH, Layout.BUTTON_SHORT), Component.translatable("sodium.options.buttons.donate"), openDonationPage, true, false);

        this.updateDisplay(parent, !SodiumClientMod.options().notifications.hasClearedDonationButton);
    }

    public void updateDisplay(VideoSettingsScreen parent, boolean enabled) {
        this.donateButtonEnabled = enabled;
        parent.setWidgetPresence(this.hideDonateButton, this.donateButtonEnabled);
        parent.setWidgetPresence(this.donateButtonText, this.donateButtonEnabled);
    }
    
    public int getWidth() {
        if (this.donateButtonEnabled) {
            return DONATE_BUTTON_WIDTH + Layout.BUTTON_SHORT + CLOSE_BUTTON_MARGIN + Layout.INNER_MARGIN * 2;
        } else {
            return 0;
        }
    }
}
