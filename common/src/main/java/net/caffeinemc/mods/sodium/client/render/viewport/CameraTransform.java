package net.caffeinemc.mods.sodium.client.render.viewport;

public class CameraTransform {
    // The integer component of the translation vector
    public final int intX, intY, intZ;

    // The fractional component of the translation vector
    public final float fracX, fracY, fracZ;

    // The original transform.
    public final double x, y, z;

    public CameraTransform(double x, double y, double z) {
        this.intX = integral(x);
        this.intY = integral(y);
        this.intZ = integral(z);

        this.fracX = fractional(x);
        this.fracY = fractional(y);
        this.fracZ = fractional(z);

        this.x = x;
        this.y = y;
        this.z = z;
    }

    private static int integral(double value) {
        return (int) value;
    }

    private static float fractional(double value) {
        return (float) (value - integral(value));
    }
}
