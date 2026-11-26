package pl.zorza.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** Mody z profilu: wczytane przez Fabric + wyłączone (.disabled) z folderu mods. */
public final class ModList {
    public static final Path TOGGLE = FabricLoader.getInstance().getConfigDir().resolve("zorza").resolve("toggle.json");
    private static final Path BUILTIN = FabricLoader.getInstance().getConfigDir().resolve("zorza").resolve("builtin.json");
    private static final Set<String> REQUIRED = Set.of("zorza", "fabric-api", "fabricloader", "minecraft", "java");
    private static final Map<String, Identifier> ICONS = new HashMap<>();
    private static Map<String, Function<Screen, Screen>> configScreens;

    private ModList() {}

    public static final class Entry {
        public String id, name, version, description, authors, file;
        public boolean loaded, enabled, builtin, required;
        public Identifier icon;
        /** id moda i wszystkich modów schowanych w jego środku (np. wersje "Universal" mają w środku właściwego moda) */
        public List<String> ids = new ArrayList<>();
    }

    private static void collectIds(ModContainer c, List<String> out, int depth) {
        out.add(c.getMetadata().getId());
        if (depth > 3) return;
        for (ModContainer sub : c.getContainedMods()) collectIds(sub, out, depth + 1);
    }

    /** Ekran ustawień dla wpisu – sprawdza też mody w środku. */
    public static Function<Screen, Screen> configScreen(Entry e) {
        if (e.ids.isEmpty()) return configScreen(e.id);
        for (String id : e.ids) {
            Function<Screen, Screen> f = configScreen(id);
            if (f != null) return f;
        }
        return null;
    }

    public static Path configFile(Entry e) {
        if (e.ids.isEmpty()) return configFile(e.id);
        for (String id : e.ids) {
            Path p = configFile(id);
            if (p != null) return p;
        }
        return null;
    }

    public static List<Entry> load() {
        Set<String> builtin = builtinFiles();
        Map<String, Boolean> pending = readToggle();
        List<Entry> out = new ArrayList<>();
        Set<String> seenFiles = new HashSet<>();
        for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
            if (c.getContainingMod().isPresent()) continue;            // moduły w środku innego moda (np. fabric-api)
            ModMetadata md = c.getMetadata();
            if (md.getType().equals("builtin")) continue;              // minecraft, java, fabricloader
            if (md.getId().startsWith("fabric-") && !md.getId().equals("fabric-api")
                    && FabricLoader.getInstance().isModLoaded("fabric-api")) continue;   // części Fabric API
            if (Set.of("mixinextras", "fabricloader", "minecraft", "java").contains(md.getId())) continue;
            Entry e = new Entry();
            e.id = md.getId();
            e.name = md.getName();
            e.version = md.getVersion().getFriendlyString();
            e.description = md.getDescription();
            e.authors = md.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", "));
            e.loaded = true;
            e.file = c.getOrigin().getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH && !c.getOrigin().getPaths().isEmpty()
                    ? c.getOrigin().getPaths().get(0).getFileName().toString() : null;
            e.builtin = e.file != null && builtin.contains(e.file);
            e.required = REQUIRED.contains(e.id) || e.file == null;
            e.enabled = e.file == null || pending.getOrDefault(e.file, true);
            e.icon = icon(c);
            collectIds(c, e.ids, 0);
            if (e.file != null) seenFiles.add(e.file);
            out.add(e);
        }
        // wyłączone mody (*.jar.disabled) – nie są wczytane, ale gracz może je włączyć
        Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        try (Stream<Path> st = Files.list(mods)) {
            for (Path f : (Iterable<Path>) st::iterator) {
                String n = f.getFileName().toString();
                if (!n.endsWith(".jar.disabled")) continue;
                String jar = n.substring(0, n.length() - ".disabled".length());
                if (seenFiles.contains(jar)) continue;
                Entry e = new Entry();
                e.file = jar;
                e.loaded = false;
                e.builtin = builtin.contains(jar);
                e.enabled = pending.getOrDefault(jar, false);
                try (ZipFile z = new ZipFile(f.toFile())) {
                    var ze = z.getEntry("fabric.mod.json");
                    if (ze != null) {
                        JsonObject o = JsonParser.parseString(new String(z.getInputStream(ze).readAllBytes())).getAsJsonObject();
                        e.id = o.has("id") ? o.get("id").getAsString() : jar;
                        e.name = o.has("name") ? o.get("name").getAsString() : e.id;
                        e.version = o.has("version") ? o.get("version").getAsString() : "";
                        e.description = o.has("description") ? o.get("description").getAsString() : "";
                    }
                } catch (Exception ignored) {
                }
                if (e.id == null) e.id = e.name = jar;
                if (e.version == null) e.version = "";
                if (e.description == null) e.description = "";
                e.authors = "";
                out.add(e);
            }
        } catch (Exception ignored) {
        }
        out.sort((x, y) -> x.name.compareToIgnoreCase(y.name));
        return out;
    }

    private static Set<String> builtinFiles() {
        Set<String> s = new HashSet<>();
        try {
            if (Files.exists(BUILTIN)) {
                JsonParser.parseString(Files.readString(BUILTIN)).getAsJsonObject().getAsJsonArray("files")
                        .forEach(x -> s.add(x.getAsString()));
            }
        } catch (Exception ignored) {
        }
        s.add("zorza-client.jar");
        return s;
    }

    public static Map<String, Boolean> readToggle() {
        Map<String, Boolean> m = new HashMap<>();
        try {
            if (Files.exists(TOGGLE)) {
                JsonParser.parseString(Files.readString(TOGGLE)).getAsJsonObject().entrySet()
                        .forEach(en -> m.put(en.getKey(), en.getValue().getAsBoolean()));
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    /** Zapisuje zmianę – launcher włączy/wyłączy plik przed następnym uruchomieniem gry. */
    public static void setEnabled(Entry e, boolean on) {
        e.enabled = on;
        Map<String, Boolean> m = readToggle();
        boolean now = e.loaded;                      // stan pliku teraz
        if (on == now) m.remove(e.file);
        else m.put(e.file, on);
        try {
            Files.createDirectories(TOGGLE.getParent());
            JsonObject o = new JsonObject();
            m.forEach(o::addProperty);
            Files.writeString(TOGGLE, o.toString());
        } catch (Exception ex) {
            ZorzaClient.LOG.warn("Zorza: nie udało się zapisać toggle.json", ex);
        }
    }

    public static boolean hasPending() {
        return !readToggle().isEmpty();
    }

    private static Identifier icon(ModContainer c) {
        String id = c.getMetadata().getId();
        if (ICONS.containsKey(id)) return ICONS.get(id);
        Identifier out = null;
        try {
            var path = c.getMetadata().getIconPath(64);
            if (path.isPresent()) {
                var p = c.findPath(path.get());
                if (p.isPresent()) {
                    try (InputStream in = Files.newInputStream(p.get())) {
                        NativeImage img = NativeImage.read(in);
                        out = Identifier.of("zorza", "modicon/" + id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"));
                        MinecraftClient.getInstance().getTextureManager().registerTexture(out, pl.zorza.client.compat.Gfx.nativeTexture(img));
                    }
                }
            }
        } catch (Throwable ignored) {
            out = null;
        }
        ICONS.put(id, out);
        return out;
    }

    /** Ekrany ustawień modów (przez API Mod Menu, jeśli mod je udostępnia). */
    public static Function<Screen, Screen> configScreen(String modId) {
        if (configScreens == null) {
            configScreens = new HashMap<>();
            try {
                for (EntrypointContainer<Object> ec : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
                    try {
                        Object api = ec.getEntrypoint();
                        Method get = findIface(api.getClass(), "getModConfigScreenFactory");
                        if (get == null) continue;
                        Object factory = get.invoke(api);
                        if (factory == null) continue;
                        Method create = findIface(factory.getClass(), "create");
                        if (create == null) continue;
                        configScreens.put(ec.getProvider().getMetadata().getId(), wrap(factory, create));
                    } catch (Throwable ignored) {
                    }
                    // mody mogą też dawać ekrany ustawień dla innych modów (getProvidedConfigScreenFactories)
                    try {
                        Object api = ec.getEntrypoint();
                        Method prov = findIface(api.getClass(), "getProvidedConfigScreenFactories");
                        if (prov != null && prov.invoke(api) instanceof Map<?, ?> m) {
                            for (Map.Entry<?, ?> me : m.entrySet()) {
                                if (me.getValue() == null || configScreens.containsKey(String.valueOf(me.getKey()))) continue;
                                Method create = findIface(me.getValue().getClass(), "create");
                                if (create != null) configScreens.put(String.valueOf(me.getKey()), wrap(me.getValue(), create));
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        Function<Screen, Screen> f = configScreens.get(modId);
        if (f == null && Set.of("sodium", "sodium-extra", "reeses-sodium-options", "iris", "immediatelyfast", "entityculling", "moreculling").contains(modId)) {
            // te mody podpinają swoje ustawienia pod ekran "Grafika"
            f = parent -> new net.minecraft.client.gui.screen.option.VideoOptionsScreen(parent, MinecraftClient.getInstance(), MinecraftClient.getInstance().options);
        }
        return f;
    }

    /** Plik albo folder z ustawieniami moda w config/ (gdy mod nie ma ekranu ustawień). */
    public static Path configFile(String modId) {
        Path dir = FabricLoader.getInstance().getConfigDir();
        String id = modId.toLowerCase(Locale.ROOT);
        for (String cand : new String[]{id, id.replace("-", ""), id.replace("-", "_")}) {
            if (Files.isDirectory(dir.resolve(cand))) return dir.resolve(cand);
            for (String ext : new String[]{".json", ".json5", ".toml", ".properties", ".yml", ".yaml", ".cfg", ".txt"}) {
                if (Files.isRegularFile(dir.resolve(cand + ext))) return dir.resolve(cand + ext);
            }
        }
        try (Stream<Path> st = Files.list(dir)) {
            return st.filter(p -> {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return n.startsWith(id + "-") || n.startsWith(id + "_") || n.startsWith(id + ".");
            }).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Function<Screen, Screen> wrap(Object factory, Method create) {
        return parent -> {
            try {
                create.setAccessible(true);
                return (Screen) create.invoke(factory, parent);
            } catch (Throwable t) {
                return null;
            }
        };
    }

    private static Method findIface(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                for (Method m : i.getMethods()) if (m.getName().equals(name) && m.getParameterCount() <= 1) return m;
            }
        }
        return null;
    }
}
