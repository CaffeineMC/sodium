package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.client.util.Dim2i;

public interface Dimensioned {
    Dim2i getDimensions();

    default int getX() {
        return this.getDimensions().x();
    }

    default int getY() {
        return this.getDimensions().y();
    }

    default int getWidth() {
        return this.getDimensions().width();
    }

    default int getHeight() {
        return this.getDimensions().height();
    }

    default int getLimitX() {
        return this.getX() + this.getWidth();
    }

    default int getLimitY() {
        return this.getY() + this.getHeight();
    }

    default int getCenterX() {
        return this.getX() + this.getWidth() / 2;
    }

    default int getCenterY() {
        return this.getY() + this.getHeight() / 2;
    }
}
