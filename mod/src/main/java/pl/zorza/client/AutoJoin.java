package pl.zorza.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import pl.zorza.client.gui.AutoJoinScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Launcher zapisuje config/zorza/autojoin.json, gdy gracz kliknie "Graj" przy serwerze.
 * Po pierwszym pokazaniu menu głównego pytamy, czy połączyć z tym serwerem.
 */
public final class AutoJoin {
    public static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zorza").resolve("autojoin.json");
    private static boolean checked = false;
    private static final java.util.Set<net.minecraft.client.gui.screen.Screen> HOOKED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private AutoJoin() {}

    public record Data(String address, String name, List<Text> motd, int players, int max, byte[] favicon) {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            MenuButtons.add(client, screen);
            if (HOOKED.add(screen))           // ponowne init (zmiana rozmiaru okna) nie dubluje zdarzenia
                ScreenEvents.beforeRender(screen).register((sc, ctx, mx, my, delta) -> SmoothScroll.frame());
            if (checked || !(screen instanceof TitleScreen)) return;
            checked = true;
            Data d = read();
            if (d != null) client.execute(() -> client.setScreen(new AutoJoinScreen(screen, d)));
        });
    }

    private static Data read() {
        try {
            if (!Files.exists(FILE)) return null;
            JsonObject o = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            Files.deleteIfExists(FILE);       // pytamy tylko raz
            long created = o.has("created") ? o.get("created").getAsLong() : 0;
            if (System.currentTimeMillis() - created > 15 * 60 * 1000L) return null;   // stary plik – pomiń
            String address = o.get("address").getAsString();
            String name = o.has("name") ? o.get("name").getAsString() : address;
            List<Text> motd = new ArrayList<>();
            if (o.has("motd") && o.get("motd").isJsonArray()) {
                for (JsonElement line : o.getAsJsonArray("motd")) motd.add(line(line.getAsJsonArray()));
            }
            int players = num(o, "players"), max = num(o, "max");
            byte[] fav = null;
            if (o.has("favicon") && o.get("favicon").isJsonPrimitive()) {
                String f = o.get("favicon").getAsString();
                int i = f.indexOf(",");
                if (i > 0) fav = Base64.getDecoder().decode(f.substring(i + 1).replace("\n", ""));
            }
            return new Data(address, name, motd, players, max, fav);
        } catch (Exception e) {
            ZorzaClient.LOG.warn("Zorza: zły plik autojoin.json", e);
            return null;
        }
    }

    private static int num(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : -1;
    }

    private static Text line(JsonArray segs) {
        MutableText t = Text.empty();
        for (JsonElement e : segs) {
            JsonObject s = e.getAsJsonObject();
            Style st = Style.EMPTY;
            if (s.has("c")) {
                try {
                    st = st.withColor(TextColor.fromRgb(Integer.parseInt(s.get("c").getAsString().substring(1), 16)));
                } catch (Exception ignored) {
                }
            }
            if (s.has("b") && s.get("b").getAsBoolean()) st = st.withBold(true);
            if (s.has("i") && s.get("i").getAsBoolean()) st = st.withItalic(true);
            if (s.has("u") && s.get("u").getAsBoolean()) st = st.withUnderline(true);
            if (s.has("s") && s.get("s").getAsBoolean()) st = st.withStrikethrough(true);
            t.append(Text.literal(s.get("t").getAsString()).setStyle(st));
        }
        return t;
    }
}
