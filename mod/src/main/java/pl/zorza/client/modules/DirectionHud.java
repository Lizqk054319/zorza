package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class DirectionHud extends TextHud {
    public DirectionHud() {
        super("direction", "Direction", "Which way you are facing.", Category.HUD, false, 0.45, 0.01);
    }

    public ItemStack icon() { return new ItemStack(Items.RECOVERY_COMPASS); }

    public String text(MinecraftClient mc) {
        if (mc.player == null) return "North";
        String n = mc.player.getHorizontalFacing().asString();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1) + "  " + Math.round(((mc.player.getYaw() % 360) + 360) % 360) + "°";
    }
}
