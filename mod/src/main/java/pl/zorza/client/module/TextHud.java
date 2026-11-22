package pl.zorza.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.zorza.client.gui.R;

/** Prosty element HUD-a z jedną linijką tekstu. */
public abstract class TextHud extends HudModule {
    protected TextHud(String id, String name, String description, Category category, boolean on, double fx, double fy) {
        super(id, name, description, category, on, fx, fy);
    }

    public abstract String text(MinecraftClient mc);

    /** Tekst można podzielić znakiem "|" na etykietę (szarą) i wartość (białą), np. "Reach|3.00|blocks". */
    @Override
    public int width(MinecraftClient mc) {
        float w = 0;
        String[] p = text(mc).split("\\|");
        for (int i = 0; i < p.length; i++) w += R.width(p[i], 9, i == 1 ? R.SEMI : R.MEDIUM) + (i > 0 ? 4 : 0);
        return (int) Math.ceil(w) + (background.value ? 16 : 2);
    }

    @Override
    public int height(MinecraftClient mc) {
        return background.value ? 18 : 10;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        float x = background.value ? 8 : 1, y = background.value ? R.midY(0, 18, 9) : 1;
        String[] p = text(mc).split("\\|");
        for (int i = 0; i < p.length; i++) {
            int col = p.length > 1 && i != 1 ? R.alpha(textColor.value, 0.7f) : textColor.value;
            if (shadow.value) R.shadowText(ctx, p[i], x, y, 9, col, i == 1 ? R.SEMI : R.MEDIUM);
            else R.text(ctx, p[i], x, y, 9, col, i == 1 ? R.SEMI : R.MEDIUM);
            x += R.width(p[i], 9, i == 1 ? R.SEMI : R.MEDIUM) + 4;
        }
    }
}
