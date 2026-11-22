package pl.zorza.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

/** "MOD MENU" – siatka modułów z kategoriami, wyszukiwarką, ulubionymi i ustawieniami. */
public class ModMenuScreen extends Screen {
    private final long opened = System.currentTimeMillis();
    private Category filter = null;
    private String search = "";
    private boolean searchFocus = false;
    private double scroll = 0, scrollTarget = 0;
    private Module settingsOf;
    private long settingsOpened;
    private Setting.Num dragging;
    private int dragX, dragW;

    // układ (liczony w layout())
    private int px, pw, top, panelY, panelBottom, gx, gy, gw, gh, cols, cw;
    private static final int CH = 82, GAP = 6;

    public ModMenuScreen(Module openSettings) {
        super(Text.literal("Zorza Mod Menu"));
        if (openSettings != null) openSettings(openSettings);
    }

    @Override
    public boolean shouldPause() { return false; }

    private void openSettings(Module m) {
        settingsOf = m;
        settingsOpened = System.currentTimeMillis();
    }

    private void layout() {
        pw = Math.min(width - 24, 600);
        px = (width - pw) / 2;
        top = 10;
        panelY = top + 56;
        panelBottom = height - 10;
        gx = px + 8;
        gy = panelY + 30;
        gw = pw - 16;
        gh = panelBottom - gy - 8;
        cols = Math.max(2, (gw + GAP) / (128 + GAP));
        cw = (gw - (cols - 1) * GAP) / cols;
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
        return Math.max(0, rows * (CH + GAP) - GAP - gh);
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        layout();
        float a = MenuSettings.anim(opened, 350);
        scroll += (scrollTarget - scroll) * 0.35;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, (1 - a) * 18, 0);

        drawTopBar(ctx, mx, my);
        drawTabs(ctx, mx, my);

        R.round(ctx, px, panelY, pw, panelBottom - panelY, 8, R.alpha(R.PANEL, 0.6f + 0.4f * a));
        if (settingsOf == null) {
            drawFilters(ctx, mx, my);
            drawGrid(ctx, mx, my);
        } else {
            drawSettings(ctx, mx, my);
        }
        ctx.getMatrices().pop();
    }

    private void drawTopBar(DrawContext ctx, int mx, int my) {
        R.logo(ctx, px, top, 22);
        ctx.drawText(textRenderer, Text.literal("MOD MENU").formatted(Formatting.BOLD), px + 28, top + 7, 0xFFFFFFFF, false);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd  hh:mm a", Locale.ENGLISH));
        int dw = R.width(date) + 16;
        R.round(ctx, px + 90, top + 1, dw, 20, 5, 0xE0101014);
        R.text(ctx, date, px + 98, top + 7, 0xFFDDDDDD);

        int right = px + pw;
        // zamknij
        boolean xh = R.in(mx, my, right - 20, top + 1, 20, 20);
        R.bordered(ctx, right - 20, top + 1, 20, 20, 5, xh ? 0xFF3A1416 : 0xE0101014, 0xFFE5484D);
        R.centered(ctx, "✕", right - 10, top + 7, 0xFFFF6B6B);
        // gracz
        if (client.player != null) {
            String name = client.player.getName().getString();
            int nw = R.width(name) + 34;
            int nx = right - 26 - nw;
            R.round(ctx, nx, top + 1, nw, 20, 5, 0xE0101014);
            PlayerSkinDrawer.draw(ctx, client.player.getSkinTextures(), nx + 4, top + 3, 16);
            R.text(ctx, name, nx + 26, top + 7, 0xFFFFFFFF);
        }
    }

    private static final String[] TABS = {"MOD MENU", "EDIT HUD", "GAME SETTINGS", "CONFIG FOLDER"};

    private int[] tabRect(int i) {
        int x = px;
        for (int j = 0; j < i; j++) x += R.width(TABS[j]) + 24 + 6;
        return new int[]{x, top + 28, R.width(TABS[i]) + 24, 20};
    }

    private void drawTabs(DrawContext ctx, int mx, int my) {
        for (int i = 0; i < TABS.length; i++) {
            int[] r = tabRect(i);
            boolean on = i == 0, h = R.in(mx, my, r[0], r[1], r[2], r[3]);
            int col = on ? R.ACCENT : h ? 0xF0202026 : 0xE0101014;
            R.round(ctx, r[0], r[1], r[2], r[3], 5, col);
            R.centered(ctx, TABS[i], r[0] + r[2] / 2, r[1] + 6, 0xFFFFFFFF);
        }
    }

    private int[] chipRect(int i) {
        int x = gx;
        for (int j = 0; j < i; j++) x += chipW(j) + 4;
        return new int[]{x, panelY + 8, chipW(i), 16};
    }

    private int chipW(int i) {
        return R.width(i == 0 ? "All" : Category.values()[i - 1].label) + 18;
    }

    private void drawFilters(DrawContext ctx, int mx, int my) {
        int n = Category.values().length + 1;
        for (int i = 0; i < n; i++) {
            int[] r = chipRect(i);
            boolean on = i == 0 ? filter == null : filter == Category.values()[i - 1];
            boolean h = R.in(mx, my, r[0], r[1], r[2], r[3]);
            R.round(ctx, r[0], r[1], r[2], r[3], 4, on ? R.ACCENT : h ? CARD_H : 0xFF1E1E24);
            R.centered(ctx, i == 0 ? "All" : Category.values()[i - 1].label, r[0] + r[2] / 2, r[1] + 4, on ? 0xFFFFFFFF : 0xFFC8C8D0);
        }
        // wyszukiwarka + ulubione
        int sw = Math.min(150, gw / 3), sx = gx + gw - sw - 20;
        R.bordered(ctx, sx, panelY + 8, sw, 16, 4, 0xFF121216, searchFocus ? R.ACCENT : 0xFF2A2A31);
        String shown = search.isEmpty() && !searchFocus ? "Search" : search + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        R.text(ctx, shown, sx + 6, panelY + 12, search.isEmpty() && !searchFocus ? R.MUTED : 0xFFFFFFFF);
        int hx = gx + gw - 16;
        boolean hh = R.in(mx, my, hx, panelY + 8, 16, 16);
        R.round(ctx, hx, panelY + 8, 16, 16, 4, Config.favoritesOnly ? R.ACCENT : hh ? CARD_H : 0xFF1E1E24);
        R.centered(ctx, "♥", hx + 8, panelY + 12, Config.favoritesOnly ? 0xFFFFFFFF : 0xFFC8C8D0);
    }

    private static final int CARD_H = 0xFF26262D;

    private void drawGrid(DrawContext ctx, int mx, int my) {
        List<Module> list = visible();
        ctx.enableScissor(gx, gy, gx + gw, gy + gh);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int x = gx + (i % cols) * (cw + GAP);
            int y = gy + (i / cols) * (CH + GAP) - (int) scroll;
            if (y + CH < gy || y > gy + gh) continue;
            float appear = MenuSettings.anim(opened + 25L * Math.min(i, 16), 300);
            int yy = y + (int) ((1 - appear) * 10);
            boolean hov = my >= gy && my < gy + gh && R.in(mx, my, x, yy, cw, CH);
            R.round(ctx, x, yy, cw, CH, 6, hov ? CARD_H : R.CARD);
            R.text(ctx, m.name, x + 8, yy + 8, m.enabled ? 0xFFFFFFFF : 0xFFB8B8C0);
            R.text(ctx, "♥", x + cw - 14, yy + 8, m.favorite ? 0xFFFF4D6D : 0xFF5A5A63);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x + cw / 2f - 12, yy + 24, 0);
            ctx.getMatrices().scale(1.5f, 1.5f, 1);
            ctx.drawItem(m.icon(), 0, 0);
            ctx.getMatrices().pop();
            boolean gh2 = R.in(mx, my, x + 6, yy + CH - 22, 16, 16);
            R.round(ctx, x + 6, yy + CH - 22, 16, 16, 4, gh2 ? 0xFF34343C : 0xFF24242B);
            R.centered(ctx, "⚙", x + 14, yy + CH - 18, 0xFFC8C8D0);
            int bx = x + 26, bw = cw - 32;
            R.round(ctx, bx, yy + CH - 22, bw, 16, 4, m.enabled ? R.GREEN : 0xFF24242B);
            R.centered(ctx, m.enabled ? "Enabled" : "Disabled", bx + bw / 2, yy + CH - 18, m.enabled ? 0xFFB9F6CA : 0xFF9A9AA3);
        }
        if (list.isEmpty()) R.centered(ctx, "Nothing found", gx + gw / 2, gy + 30, R.MUTED);
        ctx.disableScissor();
        int ms = maxScroll();
        if (ms > 0) {
            int bh = Math.max(20, gh * gh / (gh + ms));
            int by = gy + (int) ((gh - bh) * (scroll / ms));
            ctx.fill(gx + gw + 3, by, gx + gw + 5, by + bh, 0x60FFFFFF);
        }
    }

    // ------------------------------------------------------------------ ustawienia modułu

    private static final int ROW = 24;

    private void drawSettings(DrawContext ctx, int mx, int my) {
        Module m = settingsOf;
        float a = MenuSettings.anim(settingsOpened, 260);
        int x = gx + (int) ((1 - a) * 30), y = panelY + 10, w = gw;
        boolean bh = R.in(mx, my, x, y, 54, 18);
        R.round(ctx, x, y, 54, 18, 4, bh ? CARD_H : 0xFF1E1E24);
        R.text(ctx, "← Back", x + 8, y + 5, 0xFFFFFFFF);
        ctx.drawItem(m.icon(), x + 62, y + 1);
        ctx.drawText(textRenderer, Text.literal(m.name).formatted(Formatting.BOLD), x + 82, y + 5, 0xFFFFFFFF, false);
        R.text(ctx, m.description, x, y + 26, R.MUTED);

        int ry = y + 42;
        rowToggle(ctx, mx, my, x, ry, w, "Enabled", m.enabled);
        ry += ROW;
        for (Setting s : m.settings) {
            if (s instanceof Setting.Bool b) rowToggle(ctx, mx, my, x, ry, w, b.name, b.value);
            else if (s instanceof Setting.Num n) rowSlider(ctx, mx, my, x, ry, w, n);
            else if (s instanceof Setting.Color c) rowColor(ctx, mx, my, x, ry, w, c);
            ry += ROW;
        }
        ry += 6;
        int bx = x;
        bx = button(ctx, mx, my, bx, ry, "Reset settings") + 6;
        if (m instanceof HudModule) {
            bx = button(ctx, mx, my, bx, ry, "Reset position") + 6;
            button(ctx, mx, my, bx, ry, "Edit HUD");
        }
    }

    private int button(DrawContext ctx, int mx, int my, int x, int y, String label) {
        int w = R.width(label) + 18;
        R.round(ctx, x, y, w, 18, 4, R.in(mx, my, x, y, w, 18) ? CARD_H : 0xFF1E1E24);
        R.centered(ctx, label, x + w / 2, y + 5, 0xFFFFFFFF);
        return x + w;
    }

    private void rowToggle(DrawContext ctx, int mx, int my, int x, int y, int w, String label, boolean on) {
        R.round(ctx, x, y, w, ROW - 4, 5, R.CARD);
        R.text(ctx, label, x + 8, y + 6, 0xFFE6E6EA);
        int tx = x + w - 34;
        R.round(ctx, tx, y + 4, 26, 12, 6, on ? R.ACCENT : 0xFF3A3A42);
        R.round(ctx, on ? tx + 15 : tx + 1, y + 5, 10, 10, 5, 0xFFFFFFFF);
    }

    private void rowSlider(DrawContext ctx, int mx, int my, int x, int y, int w, Setting.Num n) {
        R.round(ctx, x, y, w, ROW - 4, 5, R.CARD);
        R.text(ctx, n.name, x + 8, y + 6, 0xFFE6E6EA);
        String v = n.step >= 1 ? String.valueOf((int) n.value) : String.format(Locale.ROOT, "%.2f", n.value);
        int sx = x + w / 2, sw = w / 2 - 50;
        R.text(ctx, v, x + w - 8 - R.width(v), y + 6, 0xFFFFFFFF);
        ctx.fill(sx, y + 9, sx + sw, y + 11, 0xFF3A3A42);
        int fill = (int) (sw * (n.value - n.min) / (n.max - n.min));
        ctx.fill(sx, y + 9, sx + fill, y + 11, R.ACCENT);
        R.round(ctx, sx + fill - 4, y + 6, 8, 8, 4, 0xFFFFFFFF);
    }

    private void rowColor(DrawContext ctx, int mx, int my, int x, int y, int w, Setting.Color c) {
        R.round(ctx, x, y, w, ROW - 4, 5, R.CARD);
        R.text(ctx, c.name, x + 8, y + 6, 0xFFE6E6EA);
        int n = Setting.Color.PALETTE.length, sx = x + w - 8 - n * 14;
        for (int i = 0; i < n; i++) {
            int col = Setting.Color.PALETTE[i];
            if (col == c.value) R.round(ctx, sx + i * 14 - 1, y + 3, 14, 14, 3, 0xFFFFFFFF);
            R.round(ctx, sx + i * 14, y + 4, 12, 12, 3, col);
        }
    }

    // ------------------------------------------------------------------ obsługa myszy i klawiatury

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        int right = px + pw;
        if (R.in(mx, my, right - 20, top + 1, 20, 20)) {
            close();
            return true;
        }
        for (int i = 0; i < TABS.length; i++) {
            int[] r = tabRect(i);
            if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
                switch (i) {
                    case 1 -> client.setScreen(new HudEditorScreen());
                    case 2 -> client.setScreen(new OptionsScreen(this, client.options));
                    case 3 -> Util.getOperatingSystem().open(Config.FILE.getParent().toFile());
                    default -> {
                        settingsOf = null;
                    }
                }
                return true;
            }
        }
        if (settingsOf != null) return clickSettings(mx, my);

        searchFocus = false;
        for (int i = 0; i < Category.values().length + 1; i++) {
            int[] r = chipRect(i);
            if (R.in(mx, my, r[0], r[1], r[2], r[3])) {
                filter = i == 0 ? null : Category.values()[i - 1];
                scrollTarget = 0;
                return true;
            }
        }
        int sw = Math.min(150, gw / 3), sx = gx + gw - sw - 20;
        if (R.in(mx, my, sx, panelY + 8, sw, 16)) {
            searchFocus = true;
            return true;
        }
        if (R.in(mx, my, gx + gw - 16, panelY + 8, 16, 16)) {
            Config.favoritesOnly = !Config.favoritesOnly;
            scrollTarget = 0;
            return true;
        }
        if (my < gy || my > gy + gh) return super.mouseClicked(mx, my, button);
        List<Module> list = visible();
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int x = gx + (i % cols) * (cw + GAP);
            int y = gy + (i / cols) * (CH + GAP) - (int) scroll;
            if (!R.in(mx, my, x, y, cw, CH)) continue;
            if (R.in(mx, my, x + cw - 18, y + 4, 16, 14)) {
                m.favorite = !m.favorite;
                Config.save();
            } else if (R.in(mx, my, x + 6, y + CH - 22, 16, 16) || button == 1) {
                openSettings(m);
            } else {
                m.toggle();
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean clickSettings(double mx, double my) {
        Module m = settingsOf;
        int x = gx, y = panelY + 10, w = gw;
        if (R.in(mx, my, x, y, 54, 18)) {
            settingsOf = null;
            return true;
        }
        int ry = y + 42;
        if (R.in(mx, my, x, ry, w, ROW - 4)) {
            m.toggle();
            return true;
        }
        ry += ROW;
        for (Setting s : m.settings) {
            if (R.in(mx, my, x, ry, w, ROW - 4)) {
                if (s instanceof Setting.Bool b) {
                    b.value = !b.value;
                } else if (s instanceof Setting.Num n) {
                    dragging = n;
                    dragX = x + w / 2;
                    dragW = w / 2 - 50;
                    slide(mx);
                } else if (s instanceof Setting.Color c) {
                    int cnt = Setting.Color.PALETTE.length, sx = x + w - 8 - cnt * 14;
                    int idx = (int) ((mx - sx) / 14);
                    if (mx >= sx && idx >= 0 && idx < cnt) c.value = Setting.Color.PALETTE[idx];
                }
                if (m == MenuSettings.INSTANCE) R.ACCENT = MenuSettings.INSTANCE.accent.value;
                Config.save();
                return true;
            }
            ry += ROW;
        }
        ry += 6;
        int bw1 = R.width("Reset settings") + 18;
        if (R.in(mx, my, x, ry, bw1, 18)) {
            m.resetSettings();
            Config.save();
            return true;
        }
        if (m instanceof HudModule h) {
            int bx2 = x + bw1 + 6, bw2 = R.width("Reset position") + 18;
            if (R.in(mx, my, bx2, ry, bw2, 18)) {
                h.resetPosition();
                Config.save();
                return true;
            }
            int bx3 = bx2 + bw2 + 6, bw3 = R.width("Edit HUD") + 18;
            if (R.in(mx, my, bx3, ry, bw3, 18)) {
                client.setScreen(new HudEditorScreen());
                return true;
            }
        }
        return true;
    }

    private void slide(double mx) {
        if (dragging == null || dragW <= 0) return;
        double t = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.set(dragging.min + t * (dragging.max - dragging.min));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            slide(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            Config.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (settingsOf == null) {
            scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget - v * 40));
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && settingsOf == null && search.length() < 24 && chr >= 32) {
            search += chr;
            scrollTarget = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
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
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        Config.save();
    }
}
