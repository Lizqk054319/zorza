package pl.zorza.client.gui;

import pl.zorza.client.compat.M;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;
import java.util.WeakHashMap;

/** Przyciski i suwaki Minecrafta w stylu Zora Client (rysowane w prawdziwych pikselach). */
public final class VanillaStyle {
    private static final Map<Object, float[]> HOVER = new WeakHashMap<>();

    private VanillaStyle() {}

    private static float hover(Object w, boolean on) {
        float[] h = HOVER.computeIfAbsent(w, k -> new float[1]);
        h[0] = R.approach(h[0], on ? 1f : 0f, 0.2f);
        return h[0];
    }

    private static float k() { return (float) MinecraftClient.getInstance().getWindow().getScaleFactor(); }

    public static void button(DrawContext ctx, Object widget, int x, int y, int w, int h, boolean active, boolean hovered, float alpha) {
        float t = hover(widget, hovered && active);
        float k = k();
        M.push(ctx);
        M.scale(ctx, 1f / k, 1f / k);
        int X = Math.round(x * k), Y = Math.round(y * k), W = Math.round(w * k), H = Math.round(h * k);
        int fill = active ? R.mix(0xE6111116, 0xF0202029, t) : 0xC00C0C10;
        int border = active ? R.mix(0x2EFFFFFF, R.ACCENT, t) : 0x14FFFFFF;
        R.bordered(ctx, X, Y, W, H, Math.round(Math.min(H * 0.28f, 7 * k)), Math.max(1, Math.round(k / 2f)),
                R.alpha(fill, alpha), R.alpha(border, alpha));
        M.pop(ctx);
    }

    public static void slider(DrawContext ctx, Object widget, int x, int y, int w, int h, double value, boolean active, boolean hovered, float alpha) {
        button(ctx, widget, x, y, w, h, active, hovered, alpha);
        float k = k();
        M.push(ctx);
        M.scale(ctx, 1f / k, 1f / k);
        int X = Math.round(x * k), Y = Math.round(y * k), W = Math.round(w * k), H = Math.round(h * k);
        int pad = Math.round(3 * k);
        float v = (float) Math.max(0, Math.min(1, value));
        int inner = W - 2 * pad;
        int fillW = Math.round(inner * v);
        // delikatne podświetlenie do wartości + cienki pasek na dole (nie zasłania napisu)
        if (fillW > 0) R.round(ctx, X + pad, Y + pad, Math.max(fillW, Math.round(3 * k)), H - 2 * pad,
                Math.round(Math.min((H - 2 * pad) * 0.3f, 5 * k)), R.alpha(R.mix(0x00000000, R.ACCENT, active ? 0.22f : 0.08f), alpha));
        int bh = Math.max(2, Math.round(1.2f * k)), by = Y + H - pad - bh;
        R.round(ctx, X + pad * 2, by, inner - 2 * pad, bh, bh / 2, R.alpha(0x33FFFFFF, alpha));
        int fw = Math.round((inner - 2 * pad) * v);
        if (fw > 0) R.round(ctx, X + pad * 2, by, fw, bh, bh / 2, R.alpha(active ? R.ACCENT : 0xFF5A5A64, alpha));
        int d = Math.round(3.4f * k);
        R.round(ctx, X + pad * 2 + fw - d / 2, by + bh / 2 - d / 2, d, d, d / 2, R.alpha(active ? 0xFFFFFFFF : 0xFF8C8C96, alpha));
        M.pop(ctx);
    }

    /** Napis na środku przycisku (Inter); długi napis jest pomniejszany, żeby się zmieścił. */
    /**
     * Napis przycisku. Gdy serwer używa własnych czcionek (banery, ikonki), kolorów albo nietypowych znaków,
     * rysujemy oryginalny tekst Minecrafta – inaczej zamiast banerów byłyby puste przyciski i kwadraciki.
     */
    public static void label(DrawContext ctx, net.minecraft.text.Text text, int x, int y, int w, int h, int color) {
        if (pl.zorza.client.compat.Gfx.special(text)) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int a = color >>> 24;
            int c = (a << 24) | ((color & 0xFFFFFF) == 0xFFFFFF ? 0xFFFFFF : 0xA0A0A0);
            ctx.drawCenteredTextWithShadow(tr, text, x + w / 2, y + (h - 8) / 2, c);
            return;
        }
        label(ctx, text.getString(), x, y, w, h, color);
    }

    public static void label(DrawContext ctx, String text, int x, int y, int w, int h, int color) {
        float k = k();
        float old = R.pxPerUnit;
        R.pxPerUnit = 1f;
        M.push(ctx);
        M.scale(ctx, 1f / k, 1f / k);
        float X = x * k, Y = y * k, W = w * k, H = h * k;
        float size = Math.min(H * 0.42f, 8.4f * k);
        float tw = R.width(text, size, R.SEMI);
        float max = W - 8 * k;
        if (tw > max && tw > 0) {
            size *= Math.max(0.6f, max / tw);
            tw = R.width(text, size, R.SEMI);
        }
        int c = (color & 0x00FFFFFF) == 0xFFFFFF ? color : (color & 0xFF000000) | 0x8C8C96;
        R.text(ctx, text, X + (W - tw) / 2f, R.midY(Y, H, size), size, c, R.SEMI);
        M.pop(ctx);
        R.pxPerUnit = old;
    }
}
