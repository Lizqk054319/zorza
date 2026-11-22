package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;

/** Włącza w grze opcję "Sprint: przełącz" – jak w Lunarze. */
public class ToggleSprint extends Module {
    public ToggleSprint() {
        super("toggle_sprint", "Toggle Sprint", "Press sprint once instead of holding it.", Category.UTILITY, false);
    }

    public ItemStack icon() { return new ItemStack(Items.LEATHER_BOOTS); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!mc.options.getSprintToggled().getValue()) mc.options.getSprintToggled().setValue(true);
    }

    @Override
    public void onDisable() {
        MinecraftClient.getInstance().options.getSprintToggled().setValue(false);
    }
}
