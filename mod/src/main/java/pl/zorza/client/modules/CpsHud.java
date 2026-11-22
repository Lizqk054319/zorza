package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Setting;
import pl.zorza.client.module.TextHud;

public class CpsHud extends TextHud {
    private final Setting.Bool right = add(new Setting.Bool("right", "Show right click", false));

    public CpsHud() {
        super("cps", "CPS", "Clicks per second.", Category.HUD, true, 0.01, 0.055);
    }

    public ItemStack icon() { return new ItemStack(Items.STONE_BUTTON); }

    public String text(MinecraftClient mc) {
        return right.value ? ZorzaClient.leftCps() + " | " + ZorzaClient.rightCps() + " CPS" : ZorzaClient.leftCps() + " CPS";
    }
}
