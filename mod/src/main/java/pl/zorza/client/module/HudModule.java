package pl.zorza.client.module;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.zorza.client.gui.R;

/** Element HUD-a: pozycja (ułamek ekranu), skala, tło i kolor tekstu. */
public abstract class HudModule extends Module {
    public double fx, fy, scale = 1.0;
    private final double dfx, dfy;
    public final Setting.Bool background;
    public final Setting.Color textColor;
    public final Setting.Bool shadow;

    protected HudModule(String id, String name, String description, Category category, boolean on, double fx, double fy) {
        super(id, name, description, category, on);
        this.fx = this.dfx = fx;
        this.fy = this.dfy = fy;
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
        int w = (int) Math.ceil(width(mc) * scale), h = (int) Math.ceil(height(mc) * scale);
        int x = (int) Math.round(fx * sw), y = (int) Math.round(fy * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        return new int[]{x, y, w, h};
    }

    public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int[] r = rect(mc, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        ctx.getMatrices().push();
        ctx.getMatrices().translate(r[0], r[1], 0);
        ctx.getMatrices().scale((float) scale, (float) scale, 1f);
        if (background.value) R.bordered(ctx, 0, 0, width(mc), height(mc), 5, 1, 0xC40E0E12, 0x26FFFFFF);
        draw(ctx, mc, editor);
        ctx.getMatrices().pop();
    }

    public void resetPosition() {
        fx = dfx;
        fy = dfy;
        scale = 1.0;
    }

    @Override
    public JsonObject write() {
        JsonObject o = super.write();
        o.addProperty("x", fx);
        o.addProperty("y", fy);
        o.addProperty("scale", scale);
        return o;
    }

    @Override
    public void read(JsonObject o) {
        super.read(o);
        if (o.has("x")) fx = o.get("x").getAsDouble();
        if (o.has("y")) fy = o.get("y").getAsDouble();
        if (o.has("scale")) scale = Math.max(0.5, Math.min(3.0, o.get("scale").getAsDouble()));
    }
}
