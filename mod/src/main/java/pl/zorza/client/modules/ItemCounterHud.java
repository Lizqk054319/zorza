package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;

/** Licznik ważnych przedmiotów w ekwipunku (totemy, jabłka, perły...). */
public class ItemCounterHud extends HudModule {
    private static final Item[] ITEMS = {Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENDER_PEARL,
            Items.END_CRYSTAL, Items.ARROW, Items.EXPERIENCE_BOTTLE};

    public ItemCounterHud() {
        super("item_counter", "Item Counter", "Counts totems, gapples, pearls and more.", Category.HUD, false, 0.82, 0.92);
    }

    public String icon() { return "m_items"; }

    public int width(MinecraftClient mc) { return ITEMS.length * 22 + 6; }

    public int height(MinecraftClient mc) { return 32; }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int x = 6;
        for (Item it : ITEMS) {
            int n = mc.player == null ? 0 : mc.player.getInventory().count(it);
            if (it == Items.GOLDEN_APPLE && mc.player != null) n += mc.player.getInventory().count(Items.ENCHANTED_GOLDEN_APPLE);
            ctx.drawItem(new ItemStack(it), x, 3);
            String t = String.valueOf(n);
            R.text(ctx, t, x + 8 - (int) R.width(t, 9) / 2, 21, 9, n > 0 ? textColor.value : 0xFF777777);
            x += 22;
        }
    }
}
