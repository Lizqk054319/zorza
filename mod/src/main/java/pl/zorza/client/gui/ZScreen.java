package pl.zorza.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Ekran rysowany w "jednostkach projektu": 1 jednostka ≈ 1 piksel ekranu przy 1080p,
 * niezależnie od skali GUI Minecrafta. Dzięki temu wszystko jest gładkie i ma te same proporcje.
 */
public abstract class ZScreen extends Screen {
    protected int W, H;
    private float k = 1f, gs = 1f;
    protected final long opened = System.currentTimeMillis();
    private final Map<String, Float> hovers = new HashMap<>();

    protected ZScreen(String title) {
        super(Text.literal(title));
    }

    @Override
    public boolean shouldPause() { return false; }

    private void updateScale() {
        var win = client.getWindow();
        gs = (float) win.getScaleFactor();
        k = Math.max(0.5f, win.getFramebufferHeight() / 1080f);
        W = Math.round(win.getFramebufferWidth() / k);
        H = Math.round(win.getFramebufferHeight() / k);
    }

    protected double dx(double mx) { return mx * gs / k; }

    protected double dy(double my) { return my * gs / k; }

    /** Przelicza jednostki projektu na współrzędne GUI (np. do nożyczek). */
    protected int gui(double v) { return (int) Math.round(v * k / gs); }

    protected double toGui(double v) { return v * k / gs; }

    @Override
    public final void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        updateScale();
        renderGui(ctx, mx, my, delta);
        ctx.getMatrices().push();
        float s = k / gs;
        ctx.getMatrices().scale(s, s, 1f);
        draw(ctx, dx(mx), dy(my), delta);
        ctx.getMatrices().pop();
    }

    /** Rysowanie w zwykłych współrzędnych GUI (np. elementy HUD-a). */
    protected void renderGui(DrawContext ctx, int mx, int my, float delta) {}

    protected abstract void draw(DrawContext ctx, double mx, double my, float delta);

    protected void scissor(DrawContext ctx, int x, int y, int w, int h) {
        ctx.getMatrices().push();
        ctx.getMatrices().loadIdentity();
        ctx.enableScissor(gui(x), gui(y), gui(x + w), gui(y + h));
        ctx.getMatrices().pop();
    }

    protected void endScissor(DrawContext ctx) {
        ctx.disableScissor();
    }

    /** Płynny stan najechania (0..1) dla elementu o danym kluczu. */
    protected float hover(String key, boolean over) {
        float v = hovers.getOrDefault(key, 0f);
        v = R.approach(v, over ? 1f : 0f, 0.25f);
        hovers.put(key, v);
        return v;
    }

    // przeliczanie myszy na jednostki projektu
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        updateScale();
        return click(dx(mx), dy(my), button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        return drag(dx(mx), dy(my), button) || super.mouseDragged(mx, my, button, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return release(dx(mx), dy(my), button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        return scroll(dx(mx), dy(my), v) || super.mouseScrolled(mx, my, h, v);
    }

    protected boolean click(double mx, double my, int button) { return false; }

    protected boolean drag(double mx, double my, int button) { return false; }

    protected boolean release(double mx, double my, int button) { return false; }

    protected boolean scroll(double mx, double my, double v) { return false; }
}
