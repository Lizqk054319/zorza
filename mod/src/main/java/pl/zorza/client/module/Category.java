package pl.zorza.client.module;

public enum Category {
    COMBAT("Combat"), HUD("HUD"), SOUND("Sound"), UTILITY("Utility"), VISUAL("Visual"), CLIENT("Client");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
