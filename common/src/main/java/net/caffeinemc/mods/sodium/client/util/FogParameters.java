package net.caffeinemc.mods.sodium.client.util;

public record FogParameters(float red, float green, float blue, float alpha, float environmentalStart, float environmentalEnd, float renderStart, float renderEnd) {
    public static final FogParameters NONE = new FogParameters(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE);
}
