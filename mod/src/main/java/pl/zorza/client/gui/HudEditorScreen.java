package pl.zorza.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.zorza.client.Config;
import pl.zorza.client.Modules;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Module;

import java.util.ArrayList;
import java.util.List;

/** Edytor HUD-a: przeciągnij, przewiń żeby zmienić rozmiar, prawy przycisk resetuje. */
public class HudEditorScreen extends Screen {
    private HudModule drag;
    private double offX, offY;
    private final long opened = System.currentTimeMillis();

    public HudEditorScreen() {
        super(Text.literal("Zorza HUD"));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x30000000);
    }

    private List<HudModule> huds() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : Modules.ALL) if (m.enabled && m instanceof HudModule h) out.add(h);
        return out;
    }

    private HudModule at(double mx, double my) {
        List<HudModule> l = huds();
        for (int i = l.size() - 1; i >= 0; i--) {
            int[] r = l.get(i).rect(client, width, height);
            if (R.in(mx, my, r[0], r[1], r[2], r[3])) return l.get(i);
        }
        return null;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        HudModule hover = drag != null ? drag : at(mx, my);
        for (HudModule h : huds()) {
            h.render(ctx, client, true);
            int[] r = h.rect(client, width, height);
            int col = h == hover ? R.ACCENT : 0x80FFFFFF;
            ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1], col);
            ctx.fill(r[0] - 1, r[1] + r[3], r[0] + r[2] + 1, r[1] + r[3] + 1, col);
            ctx.fill(r[0] - 1, r[1], r[0], r[1] + r[3], col);
            ctx.fill(r[0] + r[2], r[1], r[0] + r[2] + 1, r[1] + r[3], col);
            if (h == hover) R.text(ctx, h.name, r[0], Math.max(0, r[1] - 10), 0xFFFFFFFF);
        }
        float a = R.ease(opened, 300);
        String hint = "Drag to move  ·  Scroll to resize  ·  Right-click to reset";
        int w = R.width(hint) + 24;
        int y = (int) (6 - (1 - a) * 20);
        R.round(ctx, width / 2 - w / 2, y, w, 18, 8, 0xE0101014);
        R.centered(ctx, hint, width / 2, y + 5, 0xFFE6E6EA);
        int bw = 90, bx = width / 2 - bw / 2, by = height - 34;
        boolean bh = R.in(mx, my, bx, by, bw, 20);
        R.round(ctx, bx, by, bw, 20, 5, bh ? R.mix(R.ACCENT, 0xFFFFFFFF, 0.15f) : R.ACCENT);
        R.centered(ctx, "Mod Menu", width / 2, by + 6, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bw = 90, bx = width / 2 - bw / 2, by = height - 34;
        if (R.in(mx, my, bx, by, bw, 20)) {
            client.setScreen(new ModMenuScreen(null));
            return true;
        }
        HudModule h = at(mx, my);
        if (h == null) return super.mouseClicked(mx, my, button);
        if (button == 1) {
            h.resetPosition();
            Config.save();
        } else if (button == 0) {
            int[] r = h.rect(client, width, height);
            drag = h;
            offX = mx - r[0];
            offY = my - r[1];
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag != null) {
            drag.fx = Math.max(0, Math.min(1, (mx - offX) / width));
            drag.fy = Math.max(0, Math.min(1, (my - offY) / height));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag != null) {
            drag = null;
            Config.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        HudModule m = at(mx, my);
        if (m != null) {
            m.scale = Math.max(0.5, Math.min(3.0, Math.round((m.scale + v * 0.1) * 10) / 10.0));
            Config.save();
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public void removed() {
        Config.save();
    }
}
