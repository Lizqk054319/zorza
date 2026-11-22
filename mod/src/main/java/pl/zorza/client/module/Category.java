package pl.zorza.client.module;

public enum Category {
    HUD("HUD"), COMBAT("Combat"), UTILITY("Utility"), CLIENT("Client");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
