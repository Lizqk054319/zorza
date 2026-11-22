package pl.zorza.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import pl.zorza.client.Config;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    public boolean enabled;
    public boolean favorite;
    private final boolean defEnabled;

    protected Module(String id, String name, String description, Category category, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = this.defEnabled = enabledByDefault;
    }

    /** Nazwa ikonki z textures/gui/icons (albo krótki tekst, np. "3x"). */
    public abstract String icon();

    protected <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick(MinecraftClient mc) {}

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) onEnable(); else onDisable();
        Config.save();
    }

    public JsonObject write() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("favorite", favorite);
        JsonObject s = new JsonObject();
        for (Setting st : settings) s.add(st.id, st.toJson());
        o.add("settings", s);
        return o;
    }

    public void read(JsonObject o) {
        if (o.has("enabled")) enabled = o.get("enabled").getAsBoolean();
        if (o.has("favorite")) favorite = o.get("favorite").getAsBoolean();
        if (o.has("settings")) {
            JsonObject s = o.getAsJsonObject("settings");
            for (Setting st : settings) {
                JsonElement e = s.get(st.id);
                if (e != null) {
                    try {
                        st.fromJson(e);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void resetSettings() {
        for (Setting s : settings) s.reset();
        enabled = defEnabled;
    }
}
