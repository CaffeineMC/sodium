package net.caffeinemc.mods.sodium.client.util;

import org.joml.Vector4f;

public record FogParameters(float red, float green, float blue, float alpha, float environmentalStart, float environmentalEnd, float renderStart, float renderEnd, float cullDistance) {
    public static final FogParameters NONE = new FogParameters(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE);

    public FogParameters(float red, float green, float blue, float alpha, float environmentalStart, float environmentalEnd, float renderStart, float renderEnd) {
        this(red, green, blue, alpha, environmentalStart, environmentalEnd, renderStart, renderEnd, Float.isNaN(environmentalEnd) ? renderEnd : Math.min(renderEnd, environmentalEnd));
    }

    public FogParameters(Vector4f color, FogParameters old) {
        this(color.x, color.y, color.z, color.w, old.environmentalStart, old.environmentalEnd, old.renderStart, old.renderEnd);
    }
}
