package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Setting;

import java.util.ArrayList;
import java.util.List;

/** Zbroja z procentem wytrzymałości pod każdą częścią (jak na screenie). */
public class ArmorStatusHud extends HudModule {
    private final Setting.Bool percent = add(new Setting.Bool("percent", "Show percent", true));
    private final Setting.Bool hand = add(new Setting.Bool("hand", "Show held item", false));
    private final Setting.Bool vertical = add(new Setting.Bool("vertical", "Vertical", false));

    private static final ItemStack[] PREVIEW = {
            new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
            new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};

    public ArmorStatusHud() {
        super("armor_status", "Armor Status", "Armor durability in percent.", Category.HUD, true, 0.01, 0.80);
    }

    public ItemStack icon() { return new ItemStack(Items.DIAMOND_CHESTPLATE); }

    private List<ItemStack> items(MinecraftClient mc, boolean editor) {
        List<ItemStack> out = new ArrayList<>();
        if (mc.player != null) {
            for (int i = 3; i >= 0; i--) {
                ItemStack s = mc.player.getInventory().armor.get(i);
                if (!s.isEmpty()) out.add(s);
            }
            if (hand.value && !mc.player.getMainHandStack().isEmpty()) out.add(mc.player.getMainHandStack());
        }
        if (out.isEmpty() && editor) out.addAll(List.of(PREVIEW));
        return out;
    }

    private int cell() { return percent.value ? 24 : 18; }

    public int width(MinecraftClient mc) {
        int n = Math.max(1, items(mc, true).size());
        return vertical.value ? (percent.value ? 52 : 24) : n * 24 + 8;
    }

    public int height(MinecraftClient mc) {
        int n = Math.max(1, items(mc, true).size());
        return vertical.value ? n * 20 + 6 : cell() + 12;
    }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        List<ItemStack> list = items(mc, editor || mc.player == null);
        int i = 0;
        for (ItemStack s : list) {
            int x = vertical.value ? 4 : 8 + i * 24, y = vertical.value ? 4 + i * 20 : 4;
            ctx.drawItem(s, x, y);
            if (percent.value && s.isDamageable()) {
                int pct = (int) Math.round(100.0 * (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage());
                int col = pct > 60 ? 0xFF7CF0C8 : pct > 30 ? 0xFFF5D36B : 0xFFFF6B6B;
                String t = pct + "%";
                if (vertical.value) ctx.drawText(mc.textRenderer, t, x + 20, y + 4, col, shadow.value);
                else ctx.drawText(mc.textRenderer, t, x + 8 - mc.textRenderer.getWidth(t) / 2, y + 19, col, shadow.value);
            }
            i++;
        }
    }
}
