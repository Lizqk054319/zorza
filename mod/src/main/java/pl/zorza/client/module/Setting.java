package pl.zorza.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public abstract class Setting {
    public final String id;
    public final String name;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement e);

    public abstract void reset();

    public static class Bool extends Setting {
        public boolean value;
        private final boolean def;

        public Bool(String id, String name, boolean def) {
            super(id, name);
            this.value = this.def = def;
        }

        public JsonElement toJson() { return new JsonPrimitive(value); }

        public void fromJson(JsonElement e) { value = e.getAsBoolean(); }

        public void reset() { value = def; }
    }

    public static class Num extends Setting {
        public double value;
        public final double min, max, step;
        private final double def;

        public Num(String id, String name, double def, double min, double max, double step) {
            super(id, name);
            this.value = this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public void set(double v) {
            v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }

        public JsonElement toJson() { return new JsonPrimitive(value); }

        public void fromJson(JsonElement e) { set(e.getAsDouble()); }

        public void reset() { value = def; }
    }

    public static class Color extends Setting {
        public static final int[] PALETTE = {
                0xFFFFFFFF, 0xFFB0B0B8, 0xFF2F6BFF, 0xFF7C4DFF, 0xFFD946EF, 0xFFFF4D6D,
                0xFFFF7A1A, 0xFFF5B400, 0xFF22C55E, 0xFF14B8A6, 0xFF06B6D4, 0xFF000000};
        public int value;
        private final int def;

        public Color(String id, String name, int def) {
            super(id, name);
            this.value = this.def = def;
        }

        public JsonElement toJson() { return new JsonPrimitive(value); }

        public void fromJson(JsonElement e) { value = e.getAsInt(); }

        public void reset() { value = def; }
    }
}
