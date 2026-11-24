package pl.zorza.client.gui;

import pl.zorza.client.Packs;

/** Czy włączony jest ciemny ekwipunek (sprawdzane rzadko – wynik pamiętany 1 s). */
public final class DarkGui {
    private static boolean cached;
    private static long at;

    private DarkGui() {}

    public static boolean on() {
        long now = System.currentTimeMillis();
        if (now - at > 1000) {
            at = now;
            try {
                cached = Packs.enabled(Packs.DARK);
            } catch (Throwable t) {
                cached = false;
            }
        }
        return cached;
    }
}
