package pl.zorza.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Prosty element HUD-a z jedną linijką tekstu. */
public abstract class TextHud extends HudModule {
    protected TextHud(String id, String name, String description, Category category, boolean on, double fx, double fy) {
        super(id, name, description, category, on, fx, fy);
    }

    public abstract String text(MinecraftClient mc);

    @Override
    public int width(MinecraftClient mc) {
        return mc.textRenderer.getWidth(text(mc)) + (background.value ? 12 : 2);
    }

    @Override
    public int height(MinecraftClient mc) {
        return background.value ? 16 : 10;
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int pad = background.value ? 6 : 1;
        ctx.drawText(mc.textRenderer, text(mc), pad, background.value ? 4 : 1, textColor.value, shadow.value);
    }
}
