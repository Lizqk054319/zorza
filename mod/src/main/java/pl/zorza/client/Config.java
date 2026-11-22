package pl.zorza.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Module;

import java.nio.file.Files;
import java.nio.file.Path;

/** Zapis ustawień do config/zorza.json w folderze profilu. */
public final class Config {
    public static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zorza.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean favoritesOnly = false;
    public static boolean compact = false;

    private Config() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (root.has("accent")) R.ACCENT = root.get("accent").getAsInt();
            if (root.has("compact")) compact = root.get("compact").getAsBoolean();
            JsonObject mods = root.getAsJsonObject("modules");
            if (mods == null) return;
            for (Module m : Modules.ALL) {
                if (mods.has(m.id)) m.read(mods.getAsJsonObject(m.id));
            }
        } catch (Exception e) {
            ZorzaClient.LOG.warn("Zorza: nie udało się wczytać ustawień", e);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("accent", R.ACCENT);
            root.addProperty("compact", compact);
            JsonObject mods = new JsonObject();
            for (Module m : Modules.ALL) mods.add(m.id, m.write());
            root.add("modules", mods);
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) {
            ZorzaClient.LOG.warn("Zorza: nie udało się zapisać ustawień", e);
        }
    }
}
