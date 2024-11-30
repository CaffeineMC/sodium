package net.caffeinemc.mods.sodium.client.config.search;

public abstract class TextSource {
    private String text;
    private float score;

    protected abstract String getTextFromSource();

    public String getText() {
        if (this.text == null) {
            this.text = this.getTextFromSource();
        }

        return this.text;
    }

    public int getLength() {
        return this.getText().length();
    }

    public float getScore() {
        return this.score;
    }

    void setScore(float score) {
        this.score = score;
    }
}