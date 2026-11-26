package pl.zorza.client.module;

import pl.zorza.client.compat.M;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.zorza.client.gui.R;

/** Element HUD-a: pozycja (ułamek ekranu), skala, tło i kolor tekstu. */
public abstract class HudModule extends Module {
    /** Numer klatki HUD-a – do zapamiętywania obliczeń w jednej klatce. */
    public static long FRAME = 0;

    public double fx, fy;
    private final double dfx, dfy;
    public final Setting.Num size;
    public final Setting.Bool background;
    /** 0..1 – podświetlenie w edytorze HUD-a. */
    public float highlight = 0f;

    public double scale() { return size.value; }

    public void setScale(double v) { size.value = Math.max(0.5, Math.min(3.0, v)); }
    public final Setting.Color textColor;
    public final Setting.Bool shadow;

    protected HudModule(String id, String name, String description, Category category, boolean on, double fx, double fy) {
        super(id, name, description, category, on);
        this.fx = this.dfx = fx;
        this.fy = this.dfy = fy;
        size = add(new Setting.Num("size", "Size", 1.0, 0.5, 3.0, 0.05));
        background = add(new Setting.Bool("background", "Background", true));
        textColor = add(new Setting.Color("text_color", "Text color", 0xFFFFFFFF));
        shadow = add(new Setting.Bool("shadow", "Text shadow", true));
    }

    /** Szerokość i wysokość przed skalowaniem. */
    public abstract int width(MinecraftClient mc);

    public abstract int height(MinecraftClient mc);

    /** Rysuje zawartość w lokalnych współrzędnych (0,0 = lewy górny róg). */
    public abstract void draw(DrawContext ctx, MinecraftClient mc, boolean editor);

    public int[] rect(MinecraftClient mc, int sw, int sh) {
        int w = (int) Math.ceil(width(mc) * scale()), h = (int) Math.ceil(height(mc) * scale());
        int x = (int) Math.round(fx * sw), y = (int) Math.round(fy * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new int[]{x, y, w, h};
    }

    public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int[] r = rect(mc, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        M.push(ctx);
        M.translate(ctx, r[0], r[1]);
        float sc = (float) scale();
        M.scale(ctx, sc, sc);
        float k = (float) (mc.getWindow().getScaleFactor() * sc);     // fizyczne piksele na jednostkę
        R.pxPerUnit = k;
        if (background.value || (editor && highlight > 0.01f)) {
            // tło rysowane w prawdziwych pikselach – gładkie rogi i cienka ramka
            M.push(ctx);
            M.scale(ctx, 1f / k, 1f / k);
            int w = Math.round(width(mc) * k), h = Math.round(height(mc) * k);
            int rad = Math.round(Math.min(6 * k, Math.min(w, h) / 2f));
            int border = highlight > 0.01f ? R.mix(0x22FFFFFF, R.ACCENT, highlight) : 0x1EFFFFFF;
            int fill = background.value ? 0xB30C0C10 : R.alpha(0x660C0C10, highlight);
            int bw = highlight > 0.5f ? Math.max(2, Math.round(k * 0.6f)) : Math.max(1, Math.round(k * 0.34f));
            R.bordered(ctx, 0, 0, w, h, rad, bw, fill, border);
            M.pop(ctx);
        }
        draw(ctx, mc, editor);
        M.pop(ctx);
    }

    public void resetPosition() {
        fx = dfx;
        fy = dfy;
        size.value = 1.0;
    }

    @Override
    public JsonObject write() {
        JsonObject o = super.write();
        o.addProperty("x", fx);
        o.addProperty("y", fy);

        return o;
    }

    @Override
    public void read(JsonObject o) {
        super.read(o);
        if (o.has("x")) fx = o.get("x").getAsDouble();
        if (o.has("y")) fy = o.get("y").getAsDouble();
        if (o.has("scale") && !(o.has("settings") && o.getAsJsonObject("settings").has("size")))
            setScale(o.get("scale").getAsDouble());       // stary zapis rozmiaru
    }
}
