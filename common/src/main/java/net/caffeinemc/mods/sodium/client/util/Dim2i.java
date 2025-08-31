package net.caffeinemc.mods.sodium.client.util;

public record Dim2i(int x, int y, int width, int height) {
    public int getLimitX() {
        return this.x + this.width;
    }

    public int getLimitY() {
        return this.y + this.height;
    }

    public boolean containsCursor(double x, double y) {
        return x >= this.x && x < this.getLimitX() && y >= this.y && y < this.getLimitY();
    }

    public int getCenterX() {
        return this.x + (this.width / 2);
    }

    public int getCenterY() {
        return this.y + (this.height / 2);
    }
    
    public Dim2i inset(int left, int right, int top, int bottom) {
        return new Dim2i(this.x + left, this.y + top, this.width - left - right, this.height - top - bottom);
    }
    
    public Dim2i insetX(int amount) {
        return this.inset(amount, amount, 0, 0);
    }
    
    public Dim2i insetY(int amount) {
        return this.inset(0, 0, amount, amount);
    }
    
    public Dim2i insetLeft(int amount) {
        return this.inset(amount, 0, 0, 0);
    }
    
    public Dim2i insetRight(int amount) {
        return this.inset(0, amount, 0, 0);
    }
    
    public Dim2i insetTop(int amount) {
        return this.inset(0, 0, amount, 0);
    }
    
    public Dim2i insetBottom(int amount) {
        return this.inset(0, 0, 0, amount);
    }
}
