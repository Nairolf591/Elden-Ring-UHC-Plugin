package me.uhcplugin;

public enum Camp {
    DEMI_DIEUX("Demi-Dieux", "GOLD"),
    TABLE_RONDE("Table ronde", "DARK_RED"),
    SOLITAIRES("Solitaires", "GRAY");

    private final String displayName;
    private final String color;

    Camp(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }
}