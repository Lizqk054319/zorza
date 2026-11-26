package pl.zorza.client.modules;

import pl.zorza.client.compat.M;
import pl.zorza.client.compat.Ids;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ile zostało cooldownu (perła, wind charge, tarcza, chorus...). */
public class CooldownsHud extends HudModule {
    private static final int W = 150, HEAD = 18, ROW = 18;

    private record Cd(Identifier id, ItemStack stack, String name, float seconds, float progress, float alpha) {}

    /** Animacja: wiersz płynnie się pojawia i znika. [alpha, ostatnie sekundy, ostatni postęp] */
    private final Map<Identifier, float[]> anim = new LinkedHashMap<>();
    private final Map<Identifier, ItemStack> lastStack = new HashMap<>();
    private long lastFrame = System.nanoTime();

    public CooldownsHud() {
        super("cooldowns", "Cooldowns", "Shows how long until you can use an item again (ender pearl, wind charge...).", Category.COMBAT, false, 0.80, 0.62);
    }

    public String icon() { return "m_clock"; }

    private List<Cd> list(MinecraftClient mc, boolean editor) {
        List<Cd> out = new ArrayList<>();
        Map<Identifier, Float[]> active = new HashMap<>();
        if (mc.player != null) {
            try {
                var mgr = mc.player.getItemCooldownManager();
                CooldownAccessor cm = (CooldownAccessor) mgr;
                int tick = cm.zorza$tick();
                float delta = pl.zorza.client.compat.Gfx.frameDelta(mc);
                // prawdziwe przedmioty gracza – z teksturami z paczek zasobów (np. eventowe perły)
                Map<Identifier, ItemStack> owned = new HashMap<>();
                var inv = mc.player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack st = inv.getStack(i);
                    //#if MC >= 1.21.2
                    if (!st.isEmpty()) owned.putIfAbsent(mgr.getGroup(st), st);
                    //#else
                    //$$ if (!st.isEmpty()) owned.putIfAbsent(Registries.ITEM.getId(st.getItem()), st);
                    //#endif
                }
                for (Map.Entry<?, ?> raw : cm.zorza$entries().entrySet()) {
                    // klucz: grupa (Identifier) od 1.21.2, wcześniej sam przedmiot
                    Identifier key = raw.getKey() instanceof Item it ? Registries.ITEM.getId(it) : (Identifier) raw.getKey();
                    var e = Map.entry(key, raw.getValue());
                    CooldownEntryAccessor en = (CooldownEntryAccessor) e.getValue();
                    float left = (en.zorza$end() - tick - delta) / 20f;
                    if (left <= 0) continue;
                    float total = Math.max(1, en.zorza$end() - en.zorza$start()) / 20f;
                    ItemStack st = owned.get(e.getKey());
                    if (st == null) {
                        Item item = Registries.ITEM.get(e.getKey());
                        st = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
                    }
                    lastStack.put(e.getKey(), st.copyWithCount(1));
                    active.put(e.getKey(), new Float[]{left, left / total});
                }
            } catch (Throwable ignored) {
            }
        }
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        for (var e : active.entrySet()) anim.computeIfAbsent(e.getKey(), k -> new float[]{0, 0, 0});
        for (var it = anim.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            float[] a = e.getValue();
            Float[] act = active.get(e.getKey());
            a[0] = R.approach(a[0], act != null ? 1f : 0f, Math.min(1f, dt * 10f));
            if (act != null) { a[1] = act[0]; a[2] = act[1]; }
            if (act == null && a[0] <= 0.02f) { it.remove(); lastStack.remove(e.getKey()); continue; }
            ItemStack st = lastStack.getOrDefault(e.getKey(), ItemStack.EMPTY);
            String name = st.isEmpty() ? e.getKey().getPath() : st.getName().getString();
            out.add(new Cd(e.getKey(), st, name, act != null ? a[1] : 0f, act != null ? a[2] : 0f, a[0]));
        }
        if (out.isEmpty() && editor) {
            out.add(new Cd(Ids.mc("ender_pearl"), new ItemStack(Items.ENDER_PEARL), "Ender Pearl", 14.2f, 0.9f, 1f));
            //#if MC >= 1.20.5
            out.add(new Cd(Ids.mc("wind_charge"), new ItemStack(Items.WIND_CHARGE), "Wind Charge", 0.5f, 0.1f, 1f));
            //#else
            //$$ out.add(new Cd(Ids.mc("chorus_fruit"), new ItemStack(Items.CHORUS_FRUIT), "Chorus Fruit", 0.5f, 0.1f, 1f));
            //#endif
        }
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
            float al = c.alpha();
            M.push(ctx);
            M.translate(ctx, (1 - al) * 12, 0);          // wjazd z boku
            R.round(ctx, 4, y, W - 8, ROW - 2, 4, R.alpha(0x55000000, al));
            // pasek postępu w tle wiersza
            int pw = Math.round((W - 8) * Math.min(1, c.progress()));
            if (pw > 2) R.round(ctx, 4, y, pw, ROW - 2, 4, R.alpha(R.ACCENT, 0.18f * al));
            if (!c.stack().isEmpty()) {
                M.push(ctx);
                M.translate(ctx, 6, y + 1);
                M.scale(ctx, 0.875f, 0.875f);
                ctx.drawItem(c.stack(), 0, 0);
                M.pop(ctx);
            }
            String name = c.name();
            while (name.length() > 3 && R.width(name, 8.5f, R.MEDIUM) > W - 72) name = name.substring(0, name.length() - 2);
            R.text(ctx, name.equals(c.name()) ? name : name + "…", 24, y + 4.5f, 8.5f, R.alpha(textColor.value, al), R.MEDIUM);
            String t = String.format(Locale.ROOT, "%.1fs", c.seconds());
            float tw = R.width(t, 8, R.BOLD) + 10;
            R.round(ctx, (int) (W - 8 - tw), y + 2, (int) tw, ROW - 6, 6, R.alpha(R.ACCENT, al));
            R.text(ctx, t, W - 8 - tw + 5, y + 4.5f, 8, R.alpha(R.onAccent(), al), R.BOLD);
            M.pop(ctx);
            y += Math.round(ROW * Math.min(1f, al * 1.5f));
        }
    }
}
