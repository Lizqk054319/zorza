package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import pl.zorza.client.gui.R;
import pl.zorza.client.mixin.CooldownAccessor;
import pl.zorza.client.mixin.CooldownEntryAccessor;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ile zostało cooldownu (perła, wind charge, tarcza, chorus...). */
public class CooldownsHud extends HudModule {
    private static final int W = 150, HEAD = 18, ROW = 18;

    private record Cd(ItemStack stack, String name, float seconds, float progress) {}

    public CooldownsHud() {
        super("cooldowns", "Cooldowns", "Shows how long until you can use an item again (ender pearl, wind charge...).", Category.COMBAT, false, 0.80, 0.62);
    }

    public String icon() { return "m_clock"; }

    private List<Cd> list(MinecraftClient mc, boolean editor) {
        List<Cd> out = new ArrayList<>();
        if (mc.player != null) {
            try {
                CooldownAccessor cm = (CooldownAccessor) mc.player.getItemCooldownManager();
                int tick = cm.zorza$tick();
                float delta = mc.getRenderTickCounter().getTickDelta(true);
                for (Map.Entry<Identifier, ?> e : cm.zorza$entries().entrySet()) {
                    CooldownEntryAccessor en = (CooldownEntryAccessor) e.getValue();
                    float left = (en.zorza$end() - tick - delta) / 20f;
                    if (left <= 0) continue;
                    float total = Math.max(1, en.zorza$end() - en.zorza$start()) / 20f;
                    Item item = Registries.ITEM.get(e.getKey());
                    ItemStack st = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
                    String name = st.isEmpty() ? e.getKey().getPath() : st.getName().getString();
                    out.add(new Cd(st, name, left, left / total));
                }
            } catch (Throwable ignored) {
            }
        }
        if (out.isEmpty() && editor) {
            out.add(new Cd(new ItemStack(Items.ENDER_PEARL), "Ender Pearl", 14.2f, 0.9f));
            out.add(new Cd(new ItemStack(Items.WIND_CHARGE), "Wind Charge", 0.5f, 0.1f));
        }
        out.sort((a, b) -> Float.compare(b.seconds(), a.seconds()));
        return out;
    }

    public int width(MinecraftClient mc) { return W; }

    public int height(MinecraftClient mc) { return HEAD + Math.max(1, list(mc, true).size()) * ROW + 4; }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        if (!editor && list(mc, false).isEmpty()) return;
        super.render(ctx, mc, editor);
    }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        R.icon(ctx, "clock", 6, 5, 9, R.ACCENT);
        R.text(ctx, "Cooldowns", 19, 5.5f, 8.5f, textColor.value, R.SEMI);
        int y = HEAD;
        for (Cd c : list(mc, editor)) {
            R.round(ctx, 4, y, W - 8, ROW - 2, 4, 0x55000000);
            // pasek postępu w tle wiersza
            int pw = Math.round((W - 8) * Math.min(1, c.progress()));
            if (pw > 2) R.round(ctx, 4, y, pw, ROW - 2, 4, R.alpha(R.ACCENT, 0.18f));
            if (!c.stack().isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(6, y + 1, 0);
                ctx.getMatrices().scale(0.875f, 0.875f, 1f);
                ctx.drawItem(c.stack(), 0, 0);
                ctx.getMatrices().pop();
            }
            String name = c.name();
            while (name.length() > 3 && R.width(name, 8.5f, R.MEDIUM) > W - 72) name = name.substring(0, name.length() - 2);
            R.text(ctx, name.equals(c.name()) ? name : name + "…", 24, y + 4.5f, 8.5f, textColor.value, R.MEDIUM);
            String t = String.format(Locale.ROOT, "%.1fs", c.seconds());
            float tw = R.width(t, 8, R.BOLD) + 10;
            R.round(ctx, (int) (W - 8 - tw), y + 2, (int) tw, ROW - 6, 6, R.ACCENT);
            R.text(ctx, t, W - 8 - tw + 5, y + 4.5f, 8, R.onAccent(), R.BOLD);
            y += ROW;
        }
    }
}
