package pl.zorza.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import pl.zorza.client.Config;
import pl.zorza.client.Modules;
import pl.zorza.client.module.Setting;

import java.util.List;

/** Małe okno: rozmiar listy graczy (Tab), scoreboardu i paska bossa. */
public class ScaleScreen extends ZScreen {
    private static final int PW = 620, ROW = 78;
    private final Screen parent;
    private Setting.Num dragging;

    public ScaleScreen(Screen parent) {
        super("Tab & Scoreboard");
        this.parent = parent;
    }

    private List<Setting.Num> rows() {
        return List.of(Modules.HUD_SCALE.tab, Modules.HUD_SCALE.scoreboard, Modules.HUD_SCALE.bossbar);
    }

    private int px() { return (W - PW) / 2; }

    private int ph() { return 96 + rows().size() * ROW + 84; }

    private int py() { return (H - ph()) / 2; }

    private int[] track(int i) { return new int[]{px() + 40, py() + 96 + i * ROW + 44, PW - 80, 10}; }

    private int[] resetBtn() { return new int[]{px() + 40, py() + ph() - 70, 160, 46}; }

    private int[] doneBtn() { return new int[]{px() + PW - 40 - 160, py() + ph() - 70, 160, 46}; }

    @Override
    protected void draw(DrawContext ctx, double mx, double my, float delta) {
        float a = R.ease(opened, 320);
        int x = px(), y = py() + (int) ((1 - a) * 20);
        R.bordered(ctx, x, y, PW, ph(), 22, 1, R.alpha(0xF2101014, a), R.alpha(0x26FFFFFF, a));
        R.icon(ctx, "m_resize", x + 34, y + 30, 30, R.alpha(R.ACCENT, a));
        R.text(ctx, "Tab & Scoreboard", x + 80, y + 30, 26, R.alpha(0xFFFFFFFF, a), R.BOLD);
        R.text(ctx, "Size of the tab list, scoreboard and boss bar", x + 80, y + 62, 14, R.alpha(0xFF9C9CA6, a), R.MEDIUM);
        List<Setting.Num> rs = rows();
        for (int i = 0; i < rs.size(); i++) {
            Setting.Num n = rs.get(i);
            int[] t = track(i);
            t[1] += (int) ((1 - a) * 20);
            R.text(ctx, n.name, t[0], t[1] - 30, 17, R.alpha(0xFFE8E8EE, a), R.SEMI);
            String v = Math.round(n.value * 100) + "%";
            R.text(ctx, v, t[0] + t[2] - R.width(v, 17, R.BOLD), t[1] - 30, 17, R.alpha(R.ACCENT, a), R.BOLD);
            float f = (float) ((n.value - n.min) / (n.max - n.min));
            R.round(ctx, t[0], t[1], t[2], t[3], 5, R.alpha(0xFF2A2A32, a));
            R.round(ctx, t[0], t[1], Math.max(10, (int) (t[2] * f)), t[3], 5, R.alpha(R.ACCENT, a));
            float h = hover("k" + i, dragging == n || R.in(mx, my, t[0], t[1] - 14, t[2], 38));
            int k = 22 + (int) (4 * h);
            R.round(ctx, t[0] + (int) (t[2] * f) - k / 2, t[1] + 5 - k / 2, k, k, k / 2, R.alpha(0xFFFFFFFF, a));
        }
        button(ctx, mx, my, resetBtn(), "Reset", false, a, "reset");
        button(ctx, mx, my, doneBtn(), "Done", true, a, "done");
    }

    private void button(DrawContext ctx, double mx, double my, int[] b, String label, boolean primary, float a, String id) {
        float h = hover(id, R.in(mx, my, b[0], b[1], b[2], b[3]));
        int fill = primary ? R.mix(R.ACCENT, 0xFFFFFFFF, h * 0.12f) : R.mix(0xFF1E1E25, 0xFF2A2A33, h);
        R.round(ctx, b[0], b[1], b[2], b[3], 12, R.alpha(fill, a));
        R.centered(ctx, label, b[0] + b[2] / 2f, R.midY(b[1], b[3], 17), 17, R.alpha(primary ? R.onAccent() : 0xFFFFFFFF, a), R.SEMI);
    }

    private void setFrom(double mx) {
        int i = rows().indexOf(dragging);
        int[] t = track(i);
        double f = Math.max(0, Math.min(1, (mx - t[0]) / t[2]));
        dragging.set(dragging.min + f * (dragging.max - dragging.min));
    }

    @Override
    protected boolean click(double mx, double my, int button) {
        List<Setting.Num> rs = rows();
        for (int i = 0; i < rs.size(); i++) {
            int[] t = track(i);
            if (R.in(mx, my, t[0] - 12, t[1] - 16, t[2] + 24, 42)) {
                dragging = rs.get(i);
                setFrom(mx);
                return true;
            }
        }
        int[] r = resetBtn();
        if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
            rs.forEach(Setting::reset);
            Config.save();
            return true;
        }
        int[] d = doneBtn();
        if (R.in(mx, my, d[0], d[1], d[2], d[3])) close();
        return true;
    }

    @Override
    protected boolean drag(double mx, double my, int button) {
        if (dragging == null) return false;
        setFrom(mx);
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
    public void close() {
        Config.save();
        client.setScreen(parent);
    }
}
