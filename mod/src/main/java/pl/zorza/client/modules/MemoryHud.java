package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class MemoryHud extends TextHud {
    public MemoryHud() {
        super("memory", "Memory Usage", "RAM used by the game.", Category.HUD, false, 0.01, 0.40);
    }

    public ItemStack icon() { return new ItemStack(Items.REDSTONE); }

    public String text(MinecraftClient mc) {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) / 1048576, max = r.maxMemory() / 1048576;
        return "RAM " + (used * 100 / Math.max(1, max)) + "% (" + used + "/" + max + " MB)";
    }
}
