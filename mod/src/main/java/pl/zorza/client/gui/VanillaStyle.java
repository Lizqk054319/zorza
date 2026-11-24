package pl.zorza.client.gui;

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
        ctx.getMatrices().push();
        ctx.getMatrices().scale(1f / k, 1f / k, 1f);
        int X = Math.round(x * k), Y = Math.round(y * k), W = Math.round(w * k), H = Math.round(h * k);
        int fill = active ? R.mix(0xE6111116, 0xF0202029, t) : 0xC00C0C10;
        int border = active ? R.mix(0x2EFFFFFF, R.ACCENT, t) : 0x14FFFFFF;
        R.bordered(ctx, X, Y, W, H, Math.round(Math.min(H * 0.28f, 7 * k)), Math.max(1, Math.round(k / 2f)),
                R.alpha(fill, alpha), R.alpha(border, alpha));
        ctx.getMatrices().pop();
    }

    public static void slider(DrawContext ctx, Object widget, int x, int y, int w, int h, double value, boolean active, boolean hovered, float alpha) {
        button(ctx, widget, x, y, w, h, active, hovered, alpha);
        float k = k();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(1f / k, 1f / k, 1f);
        int X = Math.round(x * k), Y = Math.round(y * k), W = Math.round(w * k), H = Math.round(h * k);
        int pad = Math.round(3 * k);
        int fillW = Math.round((W - 2 * pad) * (float) Math.max(0, Math.min(1, value)));
        if (fillW > 0) R.round(ctx, X + pad, Y + pad, Math.max(fillW, Math.round(4 * k)), H - 2 * pad,
                Math.round(Math.min((H - 2 * pad) * 0.3f, 5 * k)), R.alpha(R.mix(0x55000000, R.ACCENT, active ? 0.45f : 0.15f), alpha));
        int kw = Math.round(4 * k), kx = X + pad + fillW - kw / 2;
        kx = Math.max(X + pad, Math.min(X + W - pad - kw, kx));
        R.round(ctx, kx, Y + pad, kw, H - 2 * pad, kw / 2, R.alpha(active ? 0xFFFFFFFF : 0xFF8C8C96, alpha));
        ctx.getMatrices().pop();
    }

    /** Napis na środku przycisku (Inter); długi napis jest pomniejszany, żeby się zmieścił. */
    public static void label(DrawContext ctx, String text, int x, int y, int w, int h, int color) {
        float k = k();
        float old = R.pxPerUnit;
        R.pxPerUnit = 1f;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(1f / k, 1f / k, 1f);
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
        ctx.getMatrices().pop();
        R.pxPerUnit = old;
    }
}
