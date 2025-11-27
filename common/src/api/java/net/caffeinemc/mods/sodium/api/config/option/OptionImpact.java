package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Represents the performance impact level of a configuration option.
 */
public enum OptionImpact implements NameProvider {
    /**
     * Low impact on performance. Changing this option won't affect performance in a measurable or noticeable way.
     */
    LOW(ChatFormatting.GREEN, "sodium.option_impact.low"),
    
    /**
     * Medium impact on performance. Changing this option may have a noticeable effect on performance in some scenarios and some systems.
     */
    MEDIUM(ChatFormatting.YELLOW, "sodium.option_impact.medium"),
    
    /**
     * High impact on performance. Changing this option will likely have a significant effect on performance in most scenarios.
     */
    HIGH(ChatFormatting.GOLD, "sodium.option_impact.high"),
    
    /**
     * Varies in impact on performance. The effect of changing this option on performance is highly dependent on the specific scenario and system.
     */
    VARIES(ChatFormatting.WHITE, "sodium.option_impact.varies");

    private final Component text;

    OptionImpact(ChatFormatting formatting, String text) {
        this.text = Component.translatable(text)
                .withStyle(formatting);
    }

    @Override
    public Component getName() {
        return this.text;
    }
}
