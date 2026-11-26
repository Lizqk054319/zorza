package pl.zorza.client.gui;

import pl.zorza.client.compat.M;
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
        R.pxPerUnit = gs;
        renderGui(ctx, mx, my, delta);
        R.pxPerUnit = k;
        M.push(ctx);
        float s = k / gs;
        M.scale(ctx, s, s);
        draw(ctx, dx(mx), dy(my), delta);
        M.pop(ctx);
    }

    /** Rysowanie w zwykłych współrzędnych GUI (np. elementy HUD-a). */
    protected void renderGui(DrawContext ctx, int mx, int my, float delta) {}

    protected abstract void draw(DrawContext ctx, double mx, double my, float delta);

    protected void scissor(DrawContext ctx, int x, int y, int w, int h) {
        M.push(ctx);
        M.identity(ctx);
        ctx.enableScissor(gui(x), gui(y), gui(x + w), gui(y + h));
        M.pop(ctx);
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

    // przeliczanie myszy na jednostki projektu (od 1.21.9 Minecraft podaje kliknięcia jako obiekty)
    //#if MC >= 1.21.9
    //$$ @Override
    //$$ public boolean mouseClicked(net.minecraft.client.gui.Click c, boolean doubled) {
    //$$     updateScale();
    //$$     return click(dx(c.x()), dy(c.y()), c.button()) || super.mouseClicked(c, doubled);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseDragged(net.minecraft.client.gui.Click c, double ddx, double ddy) {
    //$$     return drag(dx(c.x()), dy(c.y()), c.button()) || super.mouseDragged(c, ddx, ddy);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseReleased(net.minecraft.client.gui.Click c) {
    //$$     return release(dx(c.x()), dy(c.y()), c.button()) || super.mouseReleased(c);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean keyPressed(net.minecraft.client.input.KeyInput in) {
    //$$     return key(in.key(), in.scancode(), in.modifiers()) || super.keyPressed(in);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean charTyped(net.minecraft.client.input.CharInput in) {
    //$$     return typed((char) in.codepoint(), in.modifiers()) || super.charTyped(in);
    //$$ }
    //#else
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return key(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return typed(chr, modifiers) || super.charTyped(chr, modifiers);
    }
    //#endif

    //#if MC >= 1.20.2
    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        return scroll(dx(mx), dy(my), v) || super.mouseScrolled(mx, my, h, v);
    }
    //#else
    //$$ @Override
    //$$ public boolean mouseScrolled(double mx, double my, double v) {
    //$$     return scroll(dx(mx), dy(my), v) || super.mouseScrolled(mx, my, v);
    //$$ }
    //#endif

    /** Klawisze i znaki – ekrany Zory nadpisują te metody (niezależnie od wersji gry). */
    protected boolean key(int key, int scan, int mods) { return false; }

    protected boolean typed(char chr, int modifiers) { return false; }

    protected boolean click(double mx, double my, int button) { return false; }

    protected boolean drag(double mx, double my, int button) { return false; }

    protected boolean release(double mx, double my, int button) { return false; }

    protected boolean scroll(double mx, double my, double v) { return false; }

    private long zorza$lastSmooth = System.nanoTime();

    /** Płynne dojście do celu niezależne od FPS (przewijanie). */
    protected double smoothTo(double cur, double target) {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - zorza$lastSmooth) / 1e9);
        zorza$lastSmooth = now;
        double next = cur + (target - cur) * (1 - Math.exp(-dt * 18));
        return Math.abs(target - next) < 0.5 ? target : next;
    }
}
