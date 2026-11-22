package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectsHud extends HudModule {
    public PotionEffectsHud() {
        super("potion_effects", "Potion Effects", "Active effects with time left.", Category.HUD, false, 0.01, 0.30);
    }

    public String icon() { return "m_potion"; }

    private List<String> lines(MinecraftClient mc) {
        List<String> out = new ArrayList<>();
        if (mc.player != null && mc.world != null) {
            float tickRate = mc.world.getTickManager().getTickRate();
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String name = e.getEffectType().value().getName().getString();
                if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
                out.add(name + "  " + StatusEffectUtil.getDurationText(e, 1.0f, tickRate).getString());
            }
        }
        if (out.isEmpty()) out.add("Speed 2  1:30");
        return out;
    }

    public int width(MinecraftClient mc) {
        int w = 0;
        for (String s : lines(mc)) w = Math.max(w, (int) R.width(s, 9));
        return w + 12;
    }

    public int height(MinecraftClient mc) { return lines(mc).size() * 10 + 6; }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        if (!editor && mc.player != null && mc.player.getStatusEffects().isEmpty()) return;
        int y = 4;
        for (String s : lines(mc)) {
            R.text(ctx, s, 6, y, 9, textColor.value);
            y += 10;
        }
    }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        if (!editor && mc.player != null && mc.player.getStatusEffects().isEmpty()) return;
        super.render(ctx, mc, editor);
    }
}
