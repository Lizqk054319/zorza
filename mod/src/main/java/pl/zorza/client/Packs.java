package pl.zorza.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

/** Włączanie i wyłączanie wbudowanych paczek Zora (czcionka, ciemny ekwipunek). */
public final class Packs {
    public static final String FONT = "zora_font", DARK = "zora_dark_gui";

    private Packs() {}

    private static String id(ResourcePackManager m, String part) {
        for (ResourcePackProfile p : m.getProfiles()) if (p.getId().contains(part)) return p.getId();
        return null;
    }

    public static boolean enabled(String part) {
        ResourcePackManager m = MinecraftClient.getInstance().getResourcePackManager();
        String id = id(m, part);
        return id != null && m.getEnabledIds().contains(id);
    }

    public static boolean exists(String part) {
        return id(MinecraftClient.getInstance().getResourcePackManager(), part) != null;
    }

    /** Ustawia stan kilku paczek naraz i przeładowuje zasoby tylko raz. */
    public static void set(boolean on, String... parts) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourcePackManager m = mc.getResourcePackManager();
        boolean changed = false;
        for (String part : parts) {
            String id = id(m, part);
            if (id == null || m.getEnabledIds().contains(id) == on) continue;
            if (on) m.enable(id);
            else m.disable(id);
            changed = true;
        }
        if (changed) mc.options.refreshResourcePacks(m);
    }
}
