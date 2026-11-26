package pl.zorza.client.gui;

import pl.zorza.client.compat.M;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import pl.zorza.client.Config;
import pl.zorza.client.Modules;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;
import pl.zorza.client.modules.MenuSettings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** MOD MENU w stylu z screena: nagłówek, zakładki, kategorie, wyszukiwarka, karty modułów. */
public class ModMenuScreen extends ZScreen {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm", Locale.ROOT);

    private Category filter = null;
    private String search = "";
    private boolean searchFocus = false;
    private double scroll = 0, scrollTarget = 0;
    private Module settingsOf;
    private long settingsOpened, filterChanged = System.currentTimeMillis();
    private Setting.Num dragging;
    private int dragX, dragW;

    private int px, pw, y0, y1, y2, bottom, gx, gy, gw, gh, cols, cw, ch;
    private static final int GAP = 18;
    private static final String[] SIDE_TABS = {"gear", "list", "sparkles"};

    public ModMenuScreen(Module openSettings) {
        super("Zora Mod Menu");
        if (openSettings != null) openSettings(openSettings);
    }

    private void openSettings(Module m) {
        settingsOf = m;
        setScroll = setScrollTarget = 0;
        settingsOpened = System.currentTimeMillis();
    }

    private void layout() {
        pw = Math.min(W - 120, 1480);
        px = (W - pw) / 2;
        y0 = 40;
        y1 = y0 + 80;
        y2 = y1 + 84;
        bottom = H - 92;
        gx = px + 24;
        gy = y2 + 92;
        gw = pw - 48;
        gh = bottom - gy - 18;
        cols = Config.compact ? (gw >= 1100 ? 5 : 4) : (gw >= 1100 ? 4 : 3);
        cw = (gw - (cols - 1) * GAP) / cols;
        ch = Config.compact ? 184 : 216;
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (Module m : Modules.ALL) {
            if (filter != null && m.category != filter) continue;
            if (Config.favoritesOnly && !m.favorite) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private int maxScroll() {
        int rows = (visible().size() + cols - 1) / cols;
        return Math.max(0, rows * (ch + GAP) - GAP - gh);
    }

    // ================================================================== rysowanie

    @Override
    protected void draw(DrawContext ctx, double mx, double my, float delta) {
        layout();
        float a = MenuSettings.anim(opened, 420);
        scroll = smoothTo(scroll, scrollTarget);
        M.push(ctx);
        M.translate(ctx, 0, (1 - a) * 24);

        header(ctx, mx, my, a);
        tabs(ctx, mx, my, a);
        R.bordered(ctx, px, y2, pw, bottom - y2, 16, 1, R.alpha(0xF0101013, a), R.alpha(0x1CFFFFFF, a));
        if (settingsOf == null) {
            filters(ctx, mx, my, a);
            grid(ctx, mx, my);
        } else {
            settings(ctx, mx, my);
        }
        themePill(ctx, mx, my, a);
        M.pop(ctx);
    }

    private int[] userRect() {
        String name = client.player != null ? client.player.getName().getString() : "Player";
        int w = (int) R.width(name, 17, R.SEMI) + 92;
        return new int[]{px + pw - 82 - 82 - w, y0, w, 64};
    }

    private int[] folderRect() { return new int[]{px + pw - 82 - 72, y0, 72, 64}; }

    private int[] moveRect() { return new int[]{px + pw - 72, y0, 72, 64}; }

    private void header(DrawContext ctx, double mx, double my, float a) {
        R.logo(ctx, px, y0 + 2, 60, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "MOD MENU", px + 78, R.midY(y0, 64, 25), 25, R.alpha(0xFFFFFFFF, a), R.BOLD);
        String date = LocalDateTime.now().format(DATE_FMT);   // czas z twojego komputera (Polska: 25.11.2026  13:57)
        int dx = px + 270, dw = (int) R.width(date, 16, R.SEMI) + 80;
        R.bordered(ctx, dx, y0, dw, 64, 12, 1, R.alpha(0xF00C0C0F, a), R.alpha(0x1CFFFFFF, a));
        R.icon(ctx, "clock", dx + 24, y0 + 21, 22, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, date, dx + 58, R.midY(y0, 64, 16), 16, R.alpha(0xFFFFFFFF, a), R.SEMI);

        int[] u = userRect();
        R.bordered(ctx, u[0], u[1], u[2], u[3], 12, 1, R.alpha(0xF00C0C0F, a), R.alpha(0x1CFFFFFF, a));
        if (client.player != null) {
            //#if MC >= 1.21.9
            //$$ PlayerSkinDrawer.draw(ctx, client.player.getSkin(), u[0] + 14, u[1] + 12, 40);
            //#elseif MC >= 1.20.2
            PlayerSkinDrawer.draw(ctx, client.player.getSkinTextures(), u[0] + 14, u[1] + 12, 40);
            //#else
            //$$ PlayerSkinDrawer.draw(ctx, client.player.getSkinTexture(), u[0] + 14, u[1] + 12, 40);
            //#endif
            R.text(ctx, client.player.getName().getString(), u[0] + 70, R.midY(u[1], 64, 17), 17, R.alpha(0xFFFFFFFF, a), R.SEMI);
        }
        squareButton(ctx, mx, my, folderRect(), "folder", "hdrFolder", a, false);
        squareButton(ctx, mx, my, moveRect(), "move", "hdrMove", a, false);
    }

    private void squareButton(DrawContext ctx, double mx, double my, int[] r, String icon, String key, float a, boolean on) {
        float h = hover(key, R.in(mx, my, r[0], r[1], r[2], r[3]));
        int fill = on ? R.ACCENT : R.mix(0xF00C0C0F, 0xF0202027, h);
        R.bordered(ctx, r[0], r[1], r[2], r[3], 12, 1, R.alpha(fill, a), R.alpha(on ? 0x00000000 : 0x1CFFFFFF, a));
        int s = 26;
        R.icon(ctx, icon, r[0] + (r[2] - s) / 2, r[1] + (r[3] - s) / 2, s, R.alpha(on ? R.onAccent() : 0xFFFFFFFF, a));
    }

    private int[] modTab() { return new int[]{px, y1, 214, 64}; }

    private int[] sideTab(int i) { return new int[]{px + 214 + 16 + i * (82 + 14), y1, 82, 64}; }

    private int[] closeRect() { return new int[]{px + pw - 56, y1 + 6, 52, 52}; }

    private int[] zoraTab() { return new int[]{px + 214 + 16 + 3 * (82 + 14), y1, 214, 64}; }

    private void tabs(DrawContext ctx, double mx, double my, float a) {
        int[] t = modTab();
        float h = hover("tabMods", R.in(mx, my, t[0], t[1], t[2], t[3]));
        R.round(ctx, t[0], t[1], t[2], t[3], 12, R.alpha(R.mix(R.ACCENT, 0xFFFFFFFF, h * 0.08f), a));
        R.icon(ctx, "grid", t[0] + 30, t[1] + 21, 22, R.alpha(R.onAccent(), a));
        R.text(ctx, "MOD MENU", t[0] + 68, R.midY(t[1], 64, 17), 17, R.alpha(R.onAccent(), a), R.BOLD);
        for (int i = 0; i < 3; i++) squareButton(ctx, mx, my, sideTab(i), SIDE_TABS[i], "side" + i, a, false);
        int[] z = zoraTab();
        float hz = hover("tabZora", R.in(mx, my, z[0], z[1], z[2], z[3]));
        R.bordered(ctx, z[0], z[1], z[2], z[3], 12, 1, R.alpha(R.mix(0xF00C0C0F, 0xF0202027, hz), a), R.alpha(0x1CFFFFFF, a));
        R.logo(ctx, z[0] + 24, z[1] + 17, 30, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "ZORA MODS", z[0] + 66, R.midY(z[1], 64, 17), 17, R.alpha(0xFFFFFFFF, a), R.BOLD);
        int[] c = closeRect();
        float hc = hover("close", R.in(mx, my, c[0], c[1], c[2], c[3]));
        R.bordered(ctx, c[0], c[1], c[2], c[3], 10, 2, R.alpha(R.mix(0xF0160B0D, 0xF0471418, hc), a), R.alpha(0xFFE5484D, a));
        R.icon(ctx, "x", c[0] + 14, c[1] + 14, 24, R.alpha(0xFFFF5A5F, a));
    }

    private static String chipLabel(int i) {
        return i == 0 ? "All" : Category.values()[i - 1].label;
    }

    private int[] chip(int i) {
        int x = gx;
        for (int j = 0; j < i; j++) x += (int) R.width(chipLabel(j), 15, R.SEMI) + 52 + 10;
        return new int[]{x, y2 + 26, (int) R.width(chipLabel(i), 15, R.SEMI) + 52, 46};
    }

    private int[] searchRect() { return new int[]{gx + gw - 46 - 12 - 350, y2 + 26, 350, 46}; }

    private int[] favRect() { return new int[]{gx + gw - 46, y2 + 26, 46, 46}; }

    private void filters(DrawContext ctx, double mx, double my, float a) {
        int n = Category.values().length + 1;
        for (int i = 0; i < n; i++) {
            int[] r = chip(i);
            boolean on = i == 0 ? filter == null : filter == Category.values()[i - 1];
            float h = hover("chip" + i, R.in(mx, my, r[0], r[1], r[2], r[3]));
            R.round(ctx, r[0], r[1], r[2], r[3], 10, R.alpha(on ? R.ACCENT : R.mix(0xFF1B1B21, 0xFF26262E, h), a));
            R.centered(ctx, chipLabel(i), r[0] + r[2] / 2f, R.midY(r[1], r[3], 15), 15,
                    R.alpha(on ? R.onAccent() : 0xFFC4C4CC, a), R.SEMI);
        }
        int[] s = searchRect();
        R.bordered(ctx, s[0], s[1], s[2], s[3], 10, 1, R.alpha(0xFF1B1B21, a), R.alpha(searchFocus ? R.ACCENT : 0x14FFFFFF, a));
        boolean empty = search.isEmpty();
        String shown = empty && !searchFocus ? "Search" : search + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        R.text(ctx, shown, s[0] + 20, R.midY(s[1], s[3], 15), 15, R.alpha(empty && !searchFocus ? 0xFF8C8C96 : 0xFFFFFFFF, a), R.MEDIUM);
        R.icon(ctx, "search", s[0] + s[2] - 38, s[1] + 13, 20, R.alpha(0xFFC4C4CC, a));
        int[] f = favRect();
        float hf = hover("fav", R.in(mx, my, f[0], f[1], f[2], f[3]));
        R.round(ctx, f[0], f[1], f[2], f[3], 10, R.alpha(Config.favoritesOnly ? R.ACCENT : R.mix(0xFF1B1B21, 0xFF26262E, hf), a));
        R.icon(ctx, Config.favoritesOnly ? "heart_fill" : "heart", f[0] + 12, f[1] + 12, 22, R.alpha(Config.favoritesOnly ? R.onAccent() : 0xFFFFFFFF, a));
    }

    private int[] cardRect(int i) {
        int x = gx + (i % cols) * (cw + GAP);
        int y = gy + (i / cols) * (ch + GAP) - (int) Math.round(scroll);
        return new int[]{x, y, cw, ch};
    }

    private int[] heartRect(int[] c) { return new int[]{c[0] + c[2] - 22 - 40, c[1] + 18, 40, 40}; }

    private int[] gearRect(int[] c) { return new int[]{c[0] + 20, c[1] + c[3] - 66, 48, 48}; }

    private int[] toggleRect(int[] c) { return new int[]{c[0] + 80, c[1] + c[3] - 66, c[2] - 100, 48}; }

    private void grid(DrawContext ctx, double mx, double my) {
        List<Module> list = visible();
        scissor(ctx, gx - 4, gy - 4, gw + 8, gh + 8);
        boolean inGrid = my >= gy && my < gy + gh;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int[] c = cardRect(i);
            if (c[1] + c[3] < gy - 10 || c[1] > gy + gh + 10) continue;
            float ap = MenuSettings.anim(Math.max(opened, filterChanged) + 22L * Math.min(i, 20), 360);
            c[1] += (int) ((1 - ap) * 16);
            boolean over = inGrid && R.in(mx, my, c[0], c[1], c[2], c[3]);
            float h = hover("card" + m.id, over);
            R.bordered(ctx, c[0], c[1], c[2], c[3], 14, 1, R.alpha(R.mix(0xFF18181D, 0xFF202027, h), ap),
                    R.alpha(R.mix(0x14FFFFFF, 0x2CFFFFFF, h), ap));
            int titleCol = m.enabled ? 0xFFFFFFFF : 0xFFA2A2AB;
            R.text(ctx, m.name, c[0] + 26, c[1] + 30, 18, R.alpha(titleCol, ap), R.SEMI);
            int[] hr = heartRect(c);
            float hh = hover("heart" + m.id, inGrid && R.in(mx, my, hr[0], hr[1], hr[2], hr[3]));
            R.round(ctx, hr[0], hr[1], hr[2], hr[3], 10, R.alpha(R.mix(0xFF222229, 0xFF2E2E36, hh), ap));
            R.icon(ctx, m.favorite ? "heart_fill" : "heart", hr[0] + 10, hr[1] + 10, 20,
                    R.alpha(m.favorite ? 0xFFFF4D6D : 0xFFC4C4CC, ap));
            int iconCol = R.alpha(m.enabled ? 0xFFFFFFFF : 0xFF7E7E88, ap);
            int is = Config.compact ? 40 : 62;
            int iy = c[1] + (Config.compact ? 62 : 74);
            if (m.icon().startsWith("m_")) R.icon(ctx, m.icon(), c[0] + c[2] / 2 - is / 2, iy, is, iconCol);
            else R.centered(ctx, m.icon(), c[0] + c[2] / 2f, iy + is * 0.15f, is * 0.7f, iconCol, R.BOLD);
            int[] g = gearRect(c);
            float hg = hover("gear" + m.id, inGrid && R.in(mx, my, g[0], g[1], g[2], g[3]));
            R.round(ctx, g[0], g[1], g[2], g[3], 10, R.alpha(R.mix(0xFF222229, 0xFF30303A, hg), ap));
            R.icon(ctx, "gear", g[0] + 12, g[1] + 12, 24, R.alpha(0xFFC4C4CC, ap));
            int[] t = toggleRect(c);
            float ht = hover("tog" + m.id, inGrid && R.in(mx, my, t[0], t[1], t[2], t[3]));
            int fill = m.enabled ? R.mix(R.GREEN, 0xFF26883A, ht) : R.mix(0xFF222229, 0xFF2C2C34, ht);
            R.round(ctx, t[0], t[1], t[2], t[3], 10, R.alpha(fill, ap));
            R.centered(ctx, m.enabled ? "Enabled" : "Disabled", t[0] + t[2] / 2f, R.midY(t[1], t[3], 16), 16,
                    R.alpha(m.enabled ? R.GREEN_TEXT : 0xFF9C9CA6, ap), R.SEMI);
        }
        if (list.isEmpty()) R.centered(ctx, "Nothing found", gx + gw / 2f, gy + 60, 18, R.MUTED, R.SEMI);
        endScissor(ctx);
        int ms = maxScroll();
        if (ms > 0) {
            int bh = Math.max(60, gh * gh / (gh + ms));
            int by = gy + (int) ((gh - bh) * (scroll / ms));
            R.round(ctx, gx + gw + 8, by, 6, bh, 3, 0x50FFFFFF);
        }
    }

    // ------------------------------------------------------------------ pasek stylu na dole

    private static final String[] STYLES = {"Zora", "Compact"};

    private int[] styleRect(int i) {
        int w = 150, total = STYLES.length * w + 12;
        return new int[]{W / 2 - total / 2 + 6 + i * w, H - 72, w, 44};
    }

    private void themePill(DrawContext ctx, double mx, double my, float a) {
        int w = STYLES.length * 150 + 12;
        R.bordered(ctx, W / 2 - w / 2, H - 78, w, 56, 28, 1, R.alpha(0xF00C0C0F, a), R.alpha(0x1CFFFFFF, a));
        for (int i = 0; i < STYLES.length; i++) {
            int[] r = styleRect(i);
            boolean on = (i == 1) == Config.compact;
            float h = hover("style" + i, R.in(mx, my, r[0], r[1], r[2], r[3]));
            if (on) R.round(ctx, r[0], r[1], r[2], r[3], 22, R.alpha(R.ACCENT, a));
            else if (h > 0.02f) R.round(ctx, r[0], r[1], r[2], r[3], 22, R.alpha(0x22FFFFFF, h * a));
            R.centered(ctx, STYLES[i], r[0] + r[2] / 2f, R.midY(r[1], r[3], 15), 15,
                    R.alpha(on ? R.onAccent() : 0xFFB4B4BE, a), R.SEMI);
        }
    }

    // ================================================================== ustawienia modułu

    private static final int ROW = 62;

    private double setScroll = 0, setScrollTarget = 0;

    private int rowsBase() { return y2 + 132; }

    private int rowsTop() { return rowsBase() - (int) setScroll; }

    private int maxSetScroll() {
        if (settingsOf == null) return 0;
        int content = (1 + settingsOf.settings.size()) * ROW + 10 + 60;
        return Math.max(0, content - (bottom - rowsBase() - 12));
    }

    private void settings(DrawContext ctx, double mx, double my) {
        Module m = settingsOf;
        float a = MenuSettings.anim(settingsOpened, 320);
        int x = gx + (int) ((1 - a) * 40), w = gw;
        int[] b = {x, y2 + 26, 120, 46};
        float hb = hover("back", R.in(mx, my, b[0], b[1], b[2], b[3]));
        R.round(ctx, b[0], b[1], b[2], b[3], 10, R.alpha(R.mix(0xFF1B1B21, 0xFF26262E, hb), a));
        R.icon(ctx, "back", b[0] + 18, b[1] + 13, 20, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "Back", b[0] + 48, R.midY(b[1], b[3], 15), 15, R.alpha(0xFFFFFFFF, a), R.SEMI);
        if (m.icon().startsWith("m_")) R.icon(ctx, m.icon(), x + 140, y2 + 25, 46, R.alpha(0xFFFFFFFF, a));
        else R.text(ctx, m.icon(), x + 140, y2 + 33, 30, R.alpha(0xFFFFFFFF, a), R.BOLD);
        R.text(ctx, m.name, x + 202, y2 + 26, 24, R.alpha(0xFFFFFFFF, a), R.BOLD);
        R.text(ctx, m.description, x + 202, y2 + 58, 15, R.alpha(0xFF9C9CA6, a), R.MEDIUM);

        setScroll = smoothTo(setScroll, setScrollTarget);
        scissor(ctx, x - 4, rowsBase() - 6, w + 8, bottom - rowsBase());
        int ry = rowsTop();
        int i = 0;
        rowToggle(ctx, mx, my, x, ry, w, "Enabled", m.enabled, a, i++);
        ry += ROW;
        for (Setting s : m.settings) {
            if (s instanceof Setting.Bool bo) rowToggle(ctx, mx, my, x, ry, w, bo.name, bo.value, a, i);
            else if (s instanceof Setting.Num n) rowSlider(ctx, x, ry, w, n, a, i);
            else if (s instanceof Setting.Color c) rowColor(ctx, x, ry, w, c, a, i);
            else if (s instanceof Setting.Choice ch) rowChoice(ctx, mx, my, x, ry, w, ch, a, i);
            ry += ROW;
            i++;
        }
        ry += 10;
        int bx = x;
        for (String label : actions(m)) bx = button(ctx, mx, my, bx, ry, label, a) + 12;
        endScissor(ctx);
    }

    private List<String> actions(Module m) {
        List<String> l = new ArrayList<>(List.of("Reset settings"));
        if (m instanceof pl.zorza.client.modules.SoundModule) {
            l.add("Test sound");
            l.add("Open sounds folder");
        }
        if (m instanceof HudModule) {
            l.add("Reset position");
            l.add("Edit HUD");
        }
        return l;
    }

    private int buttonW(String label) { return (int) R.width(label, 15, R.SEMI) + 44; }

    private int button(DrawContext ctx, double mx, double my, int x, int y, String label, float a) {
        int w = buttonW(label);
        float h = hover("btn" + label, R.in(mx, my, x, y, w, 46));
        R.round(ctx, x, y, w, 46, 10, R.alpha(R.mix(0xFF1B1B21, 0xFF26262E, h), a));
        R.centered(ctx, label, x + w / 2f, R.midY(y, 46, 15), 15, R.alpha(0xFFFFFFFF, a), R.SEMI);
        return x + w;
    }

    private void rowBase(DrawContext ctx, int x, int y, int w, String label, float a, int i) {
        float ap = Math.min(a, MenuSettings.anim(settingsOpened + 30L * i, 320));
        R.round(ctx, x, y, w, ROW - 10, 12, R.alpha(0xFF18181D, ap));
        R.text(ctx, label, x + 24, R.midY(y, ROW - 10, 16), 16, R.alpha(0xFFE8E8EE, ap), R.SEMI);
    }

    private void rowToggle(DrawContext ctx, double mx, double my, int x, int y, int w, String label, boolean on, float a, int i) {
        rowBase(ctx, x, y, w, label, a, i);
        float t = hover("sw" + label, on);
        int tx = x + w - 24 - 56, ty = y + (ROW - 10 - 30) / 2;
        R.round(ctx, tx, ty, 56, 30, 15, R.mix(0xFF3A3A44, R.ACCENT, t));
        R.round(ctx, tx + 3 + (int) (26 * t), ty + 3, 24, 24, 12, R.mix(0xFFFFFFFF, R.onAccent(), t));
    }

    private int sliderX(int x, int w) { return x + w - 24 - 80 - 380; }

    private void rowSlider(DrawContext ctx, int x, int y, int w, Setting.Num n, float a, int i) {
        rowBase(ctx, x, y, w, n.name, a, i);
        int sx = sliderX(x, w), sw = 380, cy = y + (ROW - 10) / 2;
        String v = n.step >= 1 ? String.valueOf((int) n.value) : String.format(Locale.ROOT, "%.2f", n.value);
        R.text(ctx, v, x + w - 24 - R.width(v, 16, R.SEMI), R.midY(y, ROW - 10, 16), 16, 0xFFFFFFFF, R.SEMI);
        R.round(ctx, sx, cy - 3, sw, 6, 3, 0xFF34343D);
        int fill = (int) (sw * (n.value - n.min) / (n.max - n.min));
        R.round(ctx, sx, cy - 3, Math.max(6, fill), 6, 3, R.ACCENT);
        R.round(ctx, sx + fill - 10, cy - 10, 20, 20, 10, 0xFFFFFFFF);
    }

    private static final int CHOICE_W = 340;

    private void rowChoice(DrawContext ctx, double mx, double my, int x, int y, int w, Setting.Choice c, float a, int i) {
        rowBase(ctx, x, y, w, c.name, a, i);
        int bx = x + w - 24 - CHOICE_W, by = y + (ROW - 10 - 40) / 2;
        R.round(ctx, bx, by, CHOICE_W, 40, 10, 0xFF22222A);
        float hl = hover("chl" + c.id, R.in(mx, my, bx, by, 40, 40)), hr = hover("chr" + c.id, R.in(mx, my, bx + CHOICE_W - 40, by, 40, 40));
        R.round(ctx, bx, by, 40, 40, 10, R.mix(0xFF2A2A33, R.ACCENT, hl * 0.6f));
        R.round(ctx, bx + CHOICE_W - 40, by, 40, 40, 10, R.mix(0xFF2A2A33, R.ACCENT, hr * 0.6f));
        R.centered(ctx, "<", bx + 20, R.midY(by, 40, 16), 16, 0xFFFFFFFF, R.BOLD);
        R.centered(ctx, ">", bx + CHOICE_W - 20, R.midY(by, 40, 16), 16, 0xFFFFFFFF, R.BOLD);
        String v = c.value;
        while (v.length() > 4 && R.width(v, 15, R.SEMI) > CHOICE_W - 100) v = v.substring(0, v.length() - 2);
        if (!v.equals(c.value)) v = v + "…";
        R.centered(ctx, v, bx + CHOICE_W / 2f, R.midY(by, 40, 15), 15, 0xFFFFFFFF, R.SEMI);
    }

    private int swatchX(int x, int w) { return x + w - 24 - Setting.Color.PALETTE.length * 38 + 8; }

    private void rowColor(DrawContext ctx, int x, int y, int w, Setting.Color c, float a, int i) {
        rowBase(ctx, x, y, w, c.name, a, i);
        int sx = swatchX(x, w), cy = y + (ROW - 10) / 2;
        for (int k = 0; k < Setting.Color.PALETTE.length; k++) {
            int col = Setting.Color.PALETTE[k];
            if (col == c.value) R.round(ctx, sx + k * 38 - 3, cy - 16, 36, 36, 10, 0xFFFFFFFF);
            R.round(ctx, sx + k * 38, cy - 13, 30, 30, 8, col);
        }
    }

    // ================================================================== mysz i klawiatura

    @Override
    protected boolean click(double mx, double my, int button) {
        layout();
        int[] c = closeRect();
        if (R.in(mx, my, c[0], c[1], c[2], c[3])) {
            close();
            return true;
        }
        int[] f = folderRect();
        if (R.in(mx, my, f[0], f[1], f[2], f[3])) {
            Util.getOperatingSystem().open(Config.FILE.getParent().toFile());
            return true;
        }
        int[] mv = moveRect();
        if (R.in(mx, my, mv[0], mv[1], mv[2], mv[3])) {
            client.setScreen(new HudEditorScreen(false));
            return true;
        }
        int[] zt = zoraTab();
        if (R.in(mx, my, zt[0], zt[1], zt[2], zt[3])) {
            client.setScreen(new ZoraModsScreen(this));
            return true;
        }
        int[] mt = modTab();
        if (R.in(mx, my, mt[0], mt[1], mt[2], mt[3])) {
            settingsOf = null;
            return true;
        }
        for (int i = 0; i < 3; i++) {
            int[] r = sideTab(i);
            if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
                switch (i) {
                    case 0 -> client.setScreen(new OptionsScreen(this, client.options));
                    case 1 -> client.setScreen(new HudEditorScreen(true));
                    default -> openSettings(MenuSettings.INSTANCE);
                }
                return true;
            }
        }
        for (int i = 0; i < STYLES.length; i++) {
            int[] r = styleRect(i);
            if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
                Config.compact = i == 1;
                filterChanged = System.currentTimeMillis();
                scrollTarget = scroll = 0;
                Config.save();
                return true;
            }
        }
        if (settingsOf != null) return clickSettings(mx, my);

        searchFocus = false;
        for (int i = 0; i < Category.values().length + 1; i++) {
            int[] r = chip(i);
            if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
                filter = i == 0 ? null : Category.values()[i - 1];
                scrollTarget = scroll = 0;
                filterChanged = System.currentTimeMillis();
                return true;
            }
        }
        int[] s = searchRect();
        if (R.in(mx, my, s[0], s[1], s[2], s[3])) {
            searchFocus = true;
            return true;
        }
        int[] fv = favRect();
        if (R.in(mx, my, fv[0], fv[1], fv[2], fv[3])) {
            Config.favoritesOnly = !Config.favoritesOnly;
            scrollTarget = scroll = 0;
            filterChanged = System.currentTimeMillis();
            return true;
        }
        if (my < gy || my > gy + gh) return false;
        List<Module> list = visible();
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int[] cr = cardRect(i);
            if (!R.in(mx, my, cr[0], cr[1], cr[2], cr[3])) continue;
            int[] h = heartRect(cr), g = gearRect(cr), t = toggleRect(cr);
            if (R.in(mx, my, h[0], h[1], h[2], h[3])) {
                m.favorite = !m.favorite;
                Config.save();
            } else if (R.in(mx, my, g[0], g[1], g[2], g[3]) || button == 1) {
                openSettings(m);
            } else if (R.in(mx, my, t[0], t[1], t[2], t[3])) {
                m.toggle();
            } else {
                openSettings(m);
            }
            return true;
        }
        return false;
    }

    private boolean clickSettings(double mx, double my) {
        Module m = settingsOf;
        int x = gx, w = gw;
        if (R.in(mx, my, x, y2 + 26, 120, 46)) {
            settingsOf = null;
            return true;
        }
        if (my < rowsBase() - 6 || my > bottom) return true;      // poza przewijanym obszarem
        int ry = rowsTop();
        if (R.in(mx, my, x, ry, w, ROW - 10)) {
            m.toggle();
            return true;
        }
        ry += ROW;
        for (Setting s : m.settings) {
            if (R.in(mx, my, x, ry, w, ROW - 10)) {
                if (s instanceof Setting.Bool b) {
                    b.value = !b.value;
                } else if (s instanceof Setting.Num n) {
                    dragging = n;
                    dragX = sliderX(x, w);
                    dragW = 380;
                    slide(mx);
                } else if (s instanceof Setting.Choice ch) {
                    int bx = x + w - 24 - CHOICE_W;
                    if (mx < bx) return true;
                    ch.cycle(mx < bx + CHOICE_W / 2.0 ? -1 : 1);
                    if (m instanceof pl.zorza.client.modules.SoundModule sm) sm.preview();
                } else if (s instanceof Setting.Color c) {
                    int sx = swatchX(x, w), idx = (int) Math.floor((mx - sx + 4) / 38);
                    if (idx >= 0 && idx < Setting.Color.PALETTE.length) c.value = Setting.Color.PALETTE[idx];
                }
                if (m == MenuSettings.INSTANCE) R.ACCENT = MenuSettings.INSTANCE.accent.value;
                Config.save();
                return true;
            }
            ry += ROW;
        }
        ry += 10;
        int bx = x;
        for (String label : actions(m)) {
            int bw = buttonW(label);
            if (R.in(mx, my, bx, ry, bw, 46)) {
                switch (label) {
                    case "Reset settings" -> m.resetSettings();
                    case "Reset position" -> ((HudModule) m).resetPosition();
                    case "Test sound" -> ((pl.zorza.client.modules.SoundModule) m).preview();
                    case "Open sounds folder" -> pl.zorza.client.CustomSounds.openFolder();
                    default -> client.setScreen(new HudEditorScreen(false));
                }
                Config.save();
                return true;
            }
            bx += bw + 12;
        }
        return true;
    }

    private void slide(double mx) {
        if (dragging == null || dragW <= 0) return;
        double t = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.set(dragging.min + t * (dragging.max - dragging.min));
    }

    @Override
    protected boolean drag(double mx, double my, int button) {
        if (dragging == null) return false;
        slide(mx);
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
        if (settingsOf != null) {
            setScrollTarget = Math.max(0, Math.min(maxSetScroll(), setScrollTarget - v * 120));
            return true;
        }
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget - v * 170));
        return true;
    }

    @Override
    protected boolean typed(char chr, int modifiers) {
        if (searchFocus && settingsOf == null && search.length() < 24 && chr >= 32) {
            search += chr;
            scrollTarget = scroll = 0;
            return true;
        }
        return false;
    }

    @Override
    protected boolean key(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && settingsOf != null) {
            settingsOf = null;
            return true;
        }
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
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }
}
