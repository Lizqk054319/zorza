package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class ComboCounterHud extends TextHud {
    public ComboCounterHud() {
        super("combo_counter", "Combo Counter", "Hits in a row without taking damage.", Category.COMBAT, false, 0.01, 0.50);
    }

    public ItemStack icon() { return new ItemStack(Items.BLAZE_POWDER); }

    public String text(MinecraftClient mc) { return ZorzaClient.combo + "x Combo"; }
}
