package pl.zorza.client.gui;

import pl.zorza.client.compat.M;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.util.Util;
import pl.zorza.client.Config;
import pl.zorza.client.Modules;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Prawy Shift: edytowalny HUD (przeciągnij / przewiń / prawy klik = reset)
 * i na środku logo, "zora settings" oraz cztery przyciski z ikonkami.
 */
public class HudEditorScreen extends ZScreen {
    private final boolean showMenu;
    private HudModule dragging;
    private double offX, offY;
    private static final String[] ICONS = {"folder", "list", "sparkles", "gear"};
    private static final String[] TIPS = {"Config folder", "Mods", "Theme", "Game settings"};

    public HudEditorScreen(boolean showMenu) {
        super("Zora");
        this.showMenu = showMenu;
    }

    private List<HudModule> huds() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : Modules.ALL) if (m.enabled && m instanceof HudModule h) out.add(h);
        return out;
    }

    private HudModule hudAt(double gx, double gy) {
        List<HudModule> l = huds();
        for (int i = l.size() - 1; i >= 0; i--) {
            int[] r = l.get(i).rect(client, width, height);
            if (R.in(gx, gy, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4)) return l.get(i);
        }
        return null;
    }

    @Override
    protected void renderGui(DrawContext ctx, int mx, int my, float delta) {
        HudModule hov = dragging != null ? dragging : hudAt(mx, my);
        HudModule.FRAME++;
        for (HudModule h : huds()) {
            // w edytorze: delikatna ramka, po najechaniu – zaokrąglona ramka w kolorze akcentu
            h.highlight = 0.25f + 0.75f * hover("hud" + h.id, h == hov);
            h.render(ctx, client, true);
            h.highlight = 0f;
        }
    }

    // układ menu na środku (jednostki projektu)
    private int cx() { return W / 2; }

    private int cy() { return H / 2 + 10; }

    private int[] scaleBtn() { return new int[]{W - 24 - 290, 22, 290, 46}; }

    private int[] btn() { return new int[]{cx() - 130, cy() - 6, 260, 52}; }

    private int[] ico(int i) { return new int[]{cx() - 2 * 54 - 27 + i * 72 - 9 + 9, cy() + 66, 54, 54}; }

    @Override
    protected void draw(DrawContext ctx, double mx, double my, float delta) {
        float a = R.ease(opened, 450);
        // podpowiedź na górze
        String hint = "Drag to move   ·   Scroll to resize   ·   Right-click to reset";
        int hw = (int) R.width(hint, 17, R.SEMI) + 56, hy = (int) (22 - (1 - a) * 30);
        R.bordered(ctx, W / 2 - hw / 2, hy, hw, 46, 23, 1, R.alpha(0xF0101014, a), R.alpha(0x30FFFFFF, a));
        R.text(ctx, hint, W / 2f - hw / 2f + 28, R.midY(hy, 46, 17), 17, R.alpha(0xFFE8E8EE, a), R.SEMI);

        int[] sb = scaleBtn();
        sb[1] = (int) (sb[1] - (1 - a) * 30);
        float hs = hover("scale", R.in(mx, my, sb[0], sb[1], sb[2], sb[3]));
        R.bordered(ctx, sb[0], sb[1], sb[2], sb[3], 23, 1, R.alpha(R.mix(0xF0101014, 0xF0202028, hs), a), R.alpha(R.mix(0x30FFFFFF, R.ACCENT, hs), a));
        R.icon(ctx, "m_resize", sb[0] + 20, sb[1] + 12, 22, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "Tab & Scoreboard", sb[0] + 54, R.midY(sb[1], 46, 17), 17, R.alpha(0xFFE8E8EE, a), R.SEMI);

        if (!showMenu) return;
        int cx = cx(), cy = cy();
        float s = 0.9f + 0.1f * a;
        M.push(ctx);
        M.translate(ctx, cx, cy + (1 - a) * 20);
        M.scale(ctx, s, s);
        M.translate(ctx, -cx, -cy);

        R.logo(ctx, cx - 52, cy - 134, 104, R.alpha(0xFFFFFFFF, a));
        int[] b = btn();
        float hb = hover("settings", R.in(mx, my, b[0], b[1], b[2], b[3]));
        R.bordered(ctx, b[0], b[1], b[2], b[3], 12, 1, R.alpha(R.mix(0xF20C0C10, 0xF2202028, hb), a), R.alpha(0x26FFFFFF, a));
        R.centered(ctx, "zora settings", cx, R.midY(b[1], b[3], 20), 20, R.alpha(0xFFFFFFFF, a), R.SEMI);

        for (int i = 0; i < 4; i++) {
            int[] r = ico(i);
            float ai = R.ease(opened + 80 + i * 45L, 380);
            int yy = r[1] + (int) ((1 - ai) * 14);
            boolean over = R.in(mx, my, r[0], r[1], r[2], r[3]);
            float h = hover("ico" + i, over);
            R.bordered(ctx, r[0], yy, r[2], r[3], 12, 1, R.alpha(R.mix(0xF20C0C10, 0xF2202028, h), ai), R.alpha(0x26FFFFFF, ai));
            R.icon(ctx, ICONS[i], r[0] + 14, yy + 14, 26, R.alpha(0xFFFFFFFF, ai));
            if (h > 0.05f) R.centered(ctx, TIPS[i], r[0] + 27, yy + 66, 14, R.alpha(0xFFD0D0D8, h), R.MEDIUM);
        }
        M.pop(ctx);
    }

    @Override
    protected boolean click(double mx, double my, int button) {
        int[] sb = scaleBtn();
        if (R.in(mx, my, sb[0], sb[1], sb[2], sb[3])) {
            client.setScreen(new ScaleScreen(this));
            return true;
        }
        if (showMenu) {
            int[] b = btn();
            if (R.in(mx, my, b[0], b[1], b[2], b[3])) {
                client.setScreen(new ModMenuScreen(null));
                return true;
            }
            for (int i = 0; i < 4; i++) {
                int[] r = ico(i);
                if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
                    switch (i) {
                        case 0 -> Util.getOperatingSystem().open(Config.FILE.getParent().toFile());
                        case 1 -> client.setScreen(new ModMenuScreen(null));
                        case 2 -> client.setScreen(new ModMenuScreen(pl.zorza.client.modules.MenuSettings.INSTANCE));
                        default -> client.setScreen(new OptionsScreen(this, client.options));
                    }
                    return true;
                }
            }
        }
        double gx = toGui(mx), gy = toGui(my);
        HudModule h = hudAt(gx, gy);
        if (h == null) return false;
        if (button == 1) {
            h.resetPosition();
            Config.save();
        } else if (button == 0) {
            int[] r = h.rect(client, width, height);
            dragging = h;
            offX = gx - r[0];
            offY = gy - r[1];
        }
        return true;
    }

    @Override
    protected boolean drag(double mx, double my, int button) {
        if (dragging == null) return false;
        double gx = toGui(mx), gy = toGui(my);
        dragging.fx = Math.max(0, Math.min(1, (gx - offX) / width));
        dragging.fy = Math.max(0, Math.min(1, (gy - offY) / height));
        return true;
    }

    @Override
    protected boolean release(double mx, double my, int button) {
        if (dragging == null) return false;
        dragging = null;
        Config.save();
        return true;
    }

    @Override
    protected boolean scroll(double mx, double my, double v) {
        HudModule m = hudAt(toGui(mx), toGui(my));
        if (m == null) return false;
        m.setScale(Math.round((m.scale() + v * 0.1) * 10) / 10.0);
        Config.save();
        return true;
    }

    @Override
    public void removed() {
        Config.save();
    }
}
