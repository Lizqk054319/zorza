package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class FpsHud extends TextHud {
    public FpsHud() {
        super("fps", "FPS", "Shows your frames per second.", Category.HUD, true, 0.01, 0.01);
    }

    public ItemStack icon() { return new ItemStack(Items.CLOCK); }

    public String text(MinecraftClient mc) { return mc.getCurrentFps() + " FPS"; }
}
