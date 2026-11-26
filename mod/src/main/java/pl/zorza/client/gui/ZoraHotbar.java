package pl.zorza.client.gui;

import pl.zorza.client.compat.M;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Rysuje pasek przedmiotów w stylu Zora Client – w prawdziwych pikselach. */
public final class ZoraHotbar {
    private static float selX = -1;
    private static long last = System.nanoTime();

    private ZoraHotbar() {}

    private static void slot(DrawContext ctx, float k, int gx, int gy, boolean selected) {
        int x = Math.round(gx * k), y = Math.round(gy * k), s = Math.round(18 * k);
        int rad = Math.round(4 * k), b = Math.max(1, Math.round(k * 0.5f));
        R.bordered(ctx, x, y, s, s, rad, b, selected ? 0xEC1A1A22 : 0xCC101016, selected ? R.ACCENT : 0x40FFFFFF);
    }

    /** Zwraca false, gdy to nie jest element paska (wtedy rysuje Minecraft). */
    public static boolean draw(DrawContext ctx, String path, int x, int y, int w, int h) {
        float k = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        M.push(ctx);
        M.scale(ctx, 1f / k, 1f / k);
        boolean handled = true;
        switch (path) {
            case "hud/hotbar" -> {
                for (int i = 0; i < 9; i++) slot(ctx, k, x + 2 + i * 20, y + 2, false);
            }
            case "hud/hotbar_selection" -> {
                // płynne przesuwanie zaznaczenia
                long now = System.nanoTime();
                float dt = Math.min(0.05f, (now - last) / 1e9f);
                last = now;
                float target = x + 3;
                selX = selX < 0 || Math.abs(selX - target) > 200 ? target : selX + (target - selX) * (1 - (float) Math.exp(-dt * 22));
                int sx = Math.round((selX - 1) * k), sy = Math.round((y + 2) * k), s = Math.round(20 * k);
                int rad = Math.round(5 * k), b = Math.max(2, Math.round(k * 0.8f));
                R.bordered(ctx, sx, sy, s, s, rad, b, 0xEC1A1A22, R.ACCENT);
            }
            case "hud/hotbar_offhand_left" -> slot(ctx, k, x + 2, y + 3, false);
            case "hud/hotbar_offhand_right" -> slot(ctx, k, x + 9, y + 3, false);
            default -> handled = false;
        }
        M.pop(ctx);
        return handled;
    }
}
