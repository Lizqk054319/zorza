package pl.zorza.client.gui;

import pl.zorza.client.compat.M;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Przycisk w stylu Zora Client do menu Minecrafta (rysowany w prawdziwych pikselach). */
public class ZoraButton extends ClickableWidget {
    private final Runnable action;
    private final String icon;          // null = logo
    private final boolean iconOnly;
    private float hover = 0f;

    public ZoraButton(int x, int y, int w, int h, String label, String icon, boolean iconOnly, Runnable action) {
        super(x, y, w, h, Text.literal(label));
        this.action = action;
        this.icon = icon;
        this.iconOnly = iconOnly;
    }

    @Override
    //#if MC >= 1.20.2
    protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
    //#else
    //$$ protected void renderButton(DrawContext ctx, int mx, int my, float delta) {
    //#endif
        MinecraftClient mc = MinecraftClient.getInstance();
        hover = R.approach(hover, isHovered() || isFocused() ? 1f : 0f, 0.2f);
        float k = (float) mc.getWindow().getScaleFactor();
        float old = R.pxPerUnit;
        R.pxPerUnit = 1f;
        M.push(ctx);
        M.scale(ctx, 1f / k, 1f / k);
        int x = Math.round(getX() * k), y = Math.round(getY() * k), w = Math.round(getWidth() * k), h = Math.round(getHeight() * k);
        int rad = Math.round(h * 0.28f);
        R.bordered(ctx, x, y, w, h, rad, Math.max(1, Math.round(k / 2f)),
                R.mix(0xE6101014, 0xF0202028, hover), R.mix(0x30FFFFFF, R.ACCENT, hover));
        int is = Math.round(h * 0.52f);
        float ts = h * 0.40f;
        String label = getMessage().getString();
        if (iconOnly) {
            int ix = x + (w - is) / 2, iy = y + (h - is) / 2;
            if (icon == null) R.logo(ctx, ix, iy, is, 0xFFFFFFFF);
            else R.icon(ctx, icon, ix, iy, is, 0xFFFFFFFF);
        } else {
            float tw = R.width(label, ts, R.SEMI);
            if ("".equals(icon)) {                                   // sam napis
                R.text(ctx, label, x + (w - tw) / 2f, R.midY(y, h, ts), ts, 0xFFFFFFFF, R.SEMI);
                M.pop(ctx);
                R.pxPerUnit = old;
                return;
            }
            float total = is + h * 0.25f + tw;
            float sx = x + (w - total) / 2f;
            int iy = y + (h - is) / 2;
            if (icon == null) R.logo(ctx, Math.round(sx), iy, is, 0xFFFFFFFF);
            else R.icon(ctx, icon, Math.round(sx), iy, is, 0xFFFFFFFF);
            R.text(ctx, label, sx + is + h * 0.25f, R.midY(y, h, ts), ts, 0xFFFFFFFF, R.SEMI);
        }
        M.pop(ctx);
        R.pxPerUnit = old;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
