package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Setting;

import java.util.ArrayList;
import java.util.List;

/** Aktywne efekty z ikonką, nazwą i czasem. */
public class PotionEffectsHud extends HudModule {
    private final Setting.Bool icons = add(new Setting.Bool("icons", "Show icons", true));
    private static final int ROW = 13;

    private record Line(Object effect, String text, boolean ending) {}   // RegistryEntry<StatusEffect> albo StatusEffect (stare wersje)

    public PotionEffectsHud() {
        super("potion_effects", "Potion Effects", "Active effects with icon and time left.", Category.HUD, false, 0.8, 0.4);
    }

    public String icon() { return "m_potion"; }

    private List<Line> lines(MinecraftClient mc) {
        List<Line> out = new ArrayList<>();
        if (mc.player != null && mc.world != null) {
            //#if MC >= 1.20.3
            float tickRate = mc.world.getTickManager().getTickRate();
            //#endif
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                if (!e.shouldShowIcon() && !e.shouldShowParticles()) continue;
                //#if MC >= 1.20.5
                String name = e.getEffectType().value().getName().getString();
                //#else
                //$$ String name = e.getEffectType().getName().getString();
                //#endif
                if (e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
                //#if MC >= 1.20.3
                String time = StatusEffectUtil.getDurationText(e, 1.0f, tickRate).getString();
                //#else
                //$$ String time = StatusEffectUtil.getDurationText(e, 1.0f).getString();
                //#endif
                out.add(new Line(e.getEffectType(), name + "  " + time, !e.isInfinite() && e.getDuration() < 200));
            }
        }
        if (out.isEmpty()) {
            out.add(new Line(StatusEffects.SPEED, "Speed II  1:30", false));
            out.add(new Line(StatusEffects.STRENGTH, "Strength  0:08", true));
        }
        return out;
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    private int textX() { return icons.value ? 18 : 6; }

    public int width(MinecraftClient mc) {
        int w = 0;
        for (Line l : lines(mc)) w = Math.max(w, (int) Math.ceil(R.width(l.text(), 9)));
        return w + textX() + 6;
    }

    public int height(MinecraftClient mc) { return lines(mc).size() * ROW + 4; }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int y = 2;
        long blink = System.currentTimeMillis() / 300 % 2;
        for (Line l : lines(mc)) {
            if (icons.value) {
                pl.zorza.client.compat.Gfx.effectIcon(ctx, l.effect(), 5, y + 2, 9);
            }
            int col = l.ending() && blink == 0 ? R.alpha(textColor.value, 0.55f) : textColor.value;
            if (shadow.value) R.shadowText(ctx, l.text(), textX(), y + 3, 9, col, R.MEDIUM);
            else R.text(ctx, l.text(), textX(), y + 3, 9, col);
            y += ROW;
        }
    }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        if (!editor && mc.player != null && mc.player.getStatusEffects().isEmpty()) return;
        super.render(ctx, mc, editor);
    }
}
