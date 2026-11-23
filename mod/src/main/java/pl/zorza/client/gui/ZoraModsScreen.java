package pl.zorza.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import pl.zorza.client.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** ZORA MODS – mody z profilu gracza: lista, ustawienia modów i włączanie/wyłączanie. */
public class ZoraModsScreen extends ZScreen {
    private final Screen parent;
    private final List<ModList.Entry> mods;
    private String search = "";
    private boolean searchFocus = false;
    private double scroll = 0, scrollTarget = 0;
    private int px, pw, y0, ly, lh;
    private static final int ROW = 96, GAP = 12;

    public ZoraModsScreen(Screen parent) {
        super("Zora Mods");
        this.parent = parent;
        this.mods = ModList.load();
    }

    private void layout() {
        pw = Math.min(W - 120, 1280);
        px = (W - pw) / 2;
        y0 = 40;
        ly = y0 + 160;
        lh = H - ly - 70;
    }

    private List<ModList.Entry> visible() {
        String q = search.toLowerCase(Locale.ROOT).trim();
        List<ModList.Entry> out = new ArrayList<>();
        for (ModList.Entry e : mods) {
            if (q.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(q) || e.id.toLowerCase(Locale.ROOT).contains(q)) out.add(e);
        }
        return out;
    }

    private int maxScroll() { return Math.max(0, visible().size() * (ROW + GAP) - GAP - (lh - 32)); }

    private int[] closeRect() { return new int[]{px + pw - 56, y0 + 6, 52, 52}; }

    private int[] searchRect() { return new int[]{px + pw - 56 - 16 - 380, y0 + 2, 380, 60}; }

    private int[] backRect() { return new int[]{px, y0 + 80, 214, 56}; }

    @Override
    protected void draw(DrawContext ctx, double mx, double my, float delta) {
        layout();
        float a = R.ease(opened, 380);
        scroll += (scrollTarget - scroll) * 0.3;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, (1 - a) * 24, 0);

        R.logo(ctx, px, y0 + 2, 60, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "ZORA MODS", px + 78, R.midY(y0, 64, 25), 25, R.alpha(0xFFFFFFFF, a), R.BOLD);
        String cnt = mods.size() + (mods.size() == 1 ? " mod" : " mods");
        int cw = (int) R.width(cnt, 15, R.SEMI) + 32;
        R.round(ctx, px + 250, y0 + 14, cw, 36, 18, R.alpha(0xF01C1C22, a));
        R.centered(ctx, cnt, px + 250 + cw / 2f, R.midY(y0 + 14, 36, 15), 15, R.alpha(0xFFB8B8C2, a), R.SEMI);

        int[] s = searchRect();
        R.bordered(ctx, s[0], s[1], s[2], s[3], 12, 1, R.alpha(0xF00C0C0F, a), R.alpha(searchFocus ? R.ACCENT : 0x1CFFFFFF, a));
        R.icon(ctx, "search", s[0] + 18, s[1] + 19, 22, R.alpha(0xFF9C9CA6, a));
        boolean blink = searchFocus && (System.currentTimeMillis() / 500) % 2 == 0;
        R.text(ctx, search.isEmpty() && !searchFocus ? "Search mods..." : search + (blink ? "|" : ""), s[0] + 54, R.midY(s[1], 60, 16), 16,
                R.alpha(search.isEmpty() && !searchFocus ? 0xFF7C7C86 : 0xFFFFFFFF, a), R.MEDIUM);

        int[] c = closeRect();
        float hc = hover("close", R.in(mx, my, c[0], c[1], c[2], c[3]));
        R.bordered(ctx, c[0], c[1], c[2], c[3], 10, 2, R.alpha(R.mix(0xF0160B0D, 0xF0471418, hc), a), R.alpha(0xFFE5484D, a));
        R.icon(ctx, "x", c[0] + 14, c[1] + 14, 24, R.alpha(0xFFFF5A5F, a));

        int[] b = backRect();
        float hb = hover("back", R.in(mx, my, b[0], b[1], b[2], b[3]));
        R.bordered(ctx, b[0], b[1], b[2], b[3], 12, 1, R.alpha(R.mix(0xF00C0C0F, 0xF0202027, hb), a), R.alpha(0x1CFFFFFF, a));
        R.icon(ctx, "grid", b[0] + 26, b[1] + 17, 22, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "MOD MENU", b[0] + 64, R.midY(b[1], b[3], 17), 17, R.alpha(0xFFFFFFFF, a), R.BOLD);
        if (ModList.hasPending()) {
            String note = "Changes apply after restarting the game";
            R.text(ctx, note, b[0] + b[2] + 24, R.midY(b[1], b[3], 15), 15, R.alpha(0xFFFFC857, a), R.SEMI);
        }

        R.bordered(ctx, px, ly - 16, pw, lh + 16, 16, 1, R.alpha(0xF0101013, a), R.alpha(0x1CFFFFFF, a));
        List<ModList.Entry> list = visible();
        scissor(ctx, px + 2, ly, pw - 4, lh - 16);
        int x = px + 20, w = pw - 40;
        for (int i = 0; i < list.size(); i++) {
            int y = ly + i * (ROW + GAP) - (int) scroll;
            if (y + ROW < ly || y > ly + lh) continue;
            row(ctx, mx, my, list.get(i), x, y, w, a);
        }
        if (list.isEmpty()) R.centered(ctx, "No mods found", px + pw / 2f, ly + 40, 17, R.alpha(0xFF8C8C96, a), R.SEMI);
        endScissor(ctx);
        ctx.getMatrices().pop();
    }

    private final java.util.Map<String, java.util.Optional<java.nio.file.Path>> files = new java.util.HashMap<>();

    private java.nio.file.Path cfgFile(ModList.Entry e) {
        return files.computeIfAbsent(e.id, k -> java.util.Optional.ofNullable(ModList.configFile(k))).orElse(null);
    }

    private int[] switchRect(int x, int y, int w) { return new int[]{x + w - 24 - 60, y + (ROW - 32) / 2, 60, 32}; }

    private int[] gearRect(int x, int y, int w) { return new int[]{x + w - 24 - 60 - 20 - 52, y + (ROW - 52) / 2, 52, 52}; }

    private void row(DrawContext ctx, double mx, double my, ModList.Entry e, int x, int y, int w, float a) {
        boolean over = R.in(mx, my, x, y, w, ROW) && my >= ly && my <= ly + lh;
        float h = hover("row" + e.id, over);
        R.round(ctx, x, y, w, ROW, 14, R.alpha(R.mix(0xFF17171C, 0xFF1E1E25, h), a));
        float dim = e.enabled ? 1f : 0.45f;
        int ix = x + 18, iy = y + (ROW - 60) / 2;
        R.round(ctx, ix, iy, 60, 60, 12, R.alpha(0xFF24242C, a));
        if (e.icon != null) R.tex(ctx, e.icon, ix + 4, iy + 4, 52, 52, R.alpha(0xFFFFFFFF, a * dim));
        else R.centered(ctx, e.name.isEmpty() ? "?" : e.name.substring(0, 1).toUpperCase(Locale.ROOT), ix + 30, R.midY(iy, 60, 24), 24, R.alpha(0xFF9C9CA6, a), R.BOLD);

        int tx = ix + 80;
        R.text(ctx, e.name, tx, y + 18, 19, R.alpha(0xFFFFFFFF, a * dim), R.BOLD);
        float nx = tx + R.width(e.name, 19, R.BOLD) + 12;
        String ver = e.version.length() > 28 ? e.version.substring(0, 27) + "…" : e.version;
        R.text(ctx, ver, nx, y + 21, 14, R.alpha(0xFF8C8C96, a * dim), R.MEDIUM);
        nx += R.width(ver, 14, R.MEDIUM) + 12;
        String tag = e.builtin ? "BUILT-IN" : "YOUR MOD";
        int tw = (int) R.width(tag, 12, R.BOLD) + 18;
        R.round(ctx, (int) nx, y + 18, tw, 24, 8, R.alpha(e.builtin ? 0x332F7BFF : 0x2A3DDC6E, a));
        R.text(ctx, tag, nx + 9, R.midY(y + 18, 24, 12), 12, R.alpha(e.builtin ? 0xFF7FB0FF : 0xFF6FE89A, a), R.BOLD);

        int rightLimit = gearRect(x, y, w)[0] - 24;
        String desc = e.description.replace('\n', ' ');
        if (!e.authors.isEmpty()) desc = "by " + e.authors + (desc.isEmpty() ? "" : "  ·  " + desc);
        while (desc.length() > 4 && R.width(desc, 14, R.MEDIUM) > rightLimit - tx) desc = desc.substring(0, desc.length() - 2);
        if (!desc.equals(e.description) && !desc.isEmpty() && R.width(desc + "…", 14, R.MEDIUM) <= rightLimit - tx + 10) desc += "…";
        R.text(ctx, desc, tx, y + 56, 14, R.alpha(0xFF9C9CA6, a * dim), R.MEDIUM);

        Function<Screen, Screen> cfg = e.loaded ? ModList.configScreen(e.id) : null;
        java.nio.file.Path file = cfg == null ? cfgFile(e) : null;
        if (cfg != null || file != null) {
            int[] g = gearRect(x, y, w);
            boolean ov = R.in(mx, my, g[0], g[1], g[2], g[3]);
            float hg = hover("gear" + e.id, ov);
            R.bordered(ctx, g[0], g[1], g[2], g[3], 12, 1, R.alpha(R.mix(0xF00C0C0F, 0xF02A2A33, hg), a), R.alpha(0x1CFFFFFF, a));
            R.icon(ctx, cfg != null ? "gear" : "folder", g[0] + 14, g[1] + 14, 24, R.alpha(0xFFFFFFFF, a));
            if (hg > 0.05f) {
                String tip = cfg != null ? "Settings" : "Open settings file";
                R.centered(ctx, tip, g[0] + 26, g[1] - 22, 13, R.alpha(0xFFD0D0D8, a * hg), R.SEMI);
            }
        }
        int[] sw = switchRect(x, y, w);
        float t = hover("sw" + e.id, e.enabled);
        int off = e.required ? 0xFF2A2A30 : 0xFF3A3A44;
        R.round(ctx, sw[0], sw[1], sw[2], sw[3], 16, R.alpha(R.mix(off, e.required ? 0xFF3A4A6A : R.ACCENT, t), a));
        R.round(ctx, sw[0] + 4 + (int) (28 * t), sw[1] + 4, 24, 24, 12, R.alpha(e.required ? 0xFFB8B8C2 : 0xFFFFFFFF, a));
    }

    @Override
    protected boolean click(double mx, double my, int button) {
        layout();
        int[] c = closeRect();
        if (R.in(mx, my, c[0], c[1], c[2], c[3])) { close(); return true; }
        int[] b = backRect();
        if (R.in(mx, my, b[0], b[1], b[2], b[3])) { client.setScreen(new ModMenuScreen(null)); return true; }
        int[] s = searchRect();
        searchFocus = R.in(mx, my, s[0], s[1], s[2], s[3]);
        if (searchFocus || my < ly || my > ly + lh) return true;
        List<ModList.Entry> list = visible();
        int x = px + 20, w = pw - 40;
        for (int i = 0; i < list.size(); i++) {
            int y = ly + i * (ROW + GAP) - (int) scroll;
            ModList.Entry e = list.get(i);
            int[] sw = switchRect(x, y, w);
            if (R.in(mx, my, sw[0] - 8, sw[1] - 8, sw[2] + 16, sw[3] + 16)) {
                if (!e.required) ModList.setEnabled(e, !e.enabled);
                return true;
            }
            int[] g = gearRect(x, y, w);
            Function<Screen, Screen> cfg = e.loaded ? ModList.configScreen(e.id) : null;
            if (R.in(mx, my, g[0], g[1], g[2], g[3])) {
                if (cfg != null) {
                    Screen sc = cfg.apply(this);
                    if (sc != null) client.setScreen(sc);
                    return true;
                }
                java.nio.file.Path f = cfgFile(e);
                if (f != null) {
                    net.minecraft.util.Util.getOperatingSystem().open(f.toFile());
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    protected boolean scroll(double mx, double my, double v) {
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget - v * 80));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && search.length() < 24 && chr >= 32) {
            search += chr;
            scrollTarget = scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocus = false;
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_F && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchFocus = true;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
