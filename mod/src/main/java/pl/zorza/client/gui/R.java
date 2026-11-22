package pl.zorza.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Pomocnicze rysowanie: zaokrąglone prostokąty, tekst, logo, animacje. */
public final class R {
    public static final Identifier LOGO = Identifier.of("zorza", "textures/gui/logo.png");
    public static int ACCENT = 0xFF2F6BFF;
    public static final int PANEL = 0xF0141418;
    public static final int CARD = 0xFF1B1B20;
    public static final int CARD_HOVER = 0xFF24242B;
    public static final int MUTED = 0xFF9A9AA3;
    public static final int GREEN = 0xFF2E7D32;

    private R() {}

    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        round(c, x, y, w, h, r, color);
    }

    /** Ramka: rysuje kolor obramowania, a potem wnętrze. */
    public static void bordered(DrawContext c, int x, int y, int w, int h, int r, int fill, int border) {
        round(c, x, y, w, h, r, border);
        round(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    public static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static void text(DrawContext c, String s, int x, int y, int color) {
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, false);
    }

    public static void text(DrawContext c, Text s, int x, int y, int color) {
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, false);
    }

    public static void centered(DrawContext c, String s, int cx, int y, int color) {
        int w = MinecraftClient.getInstance().textRenderer.getWidth(s);
        text(c, s, cx - w / 2, y, color);
    }

    public static int width(String s) {
        return MinecraftClient.getInstance().textRenderer.getWidth(s);
    }

    public static void logo(DrawContext c, int x, int y, int size) {
        c.drawTexture(RenderLayer::getGuiTextured, LOGO, x, y, 0f, 0f, size, size, size, size);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** Płynne wejście: 0 -> 1, z wygaszeniem (ease-out cubic). */
    public static float ease(long start, float ms) {
        float t = Math.min(1f, (System.currentTimeMillis() - start) / ms);
        return 1f - (float) Math.pow(1f - t, 3);
    }
}
