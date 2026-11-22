package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

public class CustomCrosshair extends Module {
    private final Setting.Num size = add(new Setting.Num("size", "Length", 5, 1, 15, 1));
    private final Setting.Num gap = add(new Setting.Num("gap", "Gap", 2, 0, 10, 1));
    private final Setting.Num thickness = add(new Setting.Num("thickness", "Thickness", 1, 1, 4, 1));
    private final Setting.Bool dot = add(new Setting.Bool("dot", "Center dot", false));
    private final Setting.Bool outline = add(new Setting.Bool("outline", "Outline", true));
    private final Setting.Color color = add(new Setting.Color("color", "Color", 0xFFFFFFFF));

    public CustomCrosshair() {
        super("crosshair", "Crosshair", "Your own crosshair shape and color.", Category.UTILITY, false);
    }

    public ItemStack icon() { return new ItemStack(Items.TARGET); }

    private void bar(DrawContext c, int x1, int y1, int x2, int y2) {
        if (outline.value) c.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xC0000000);
        c.fill(x1, y1, x2, y2, color.value);
    }

    public void drawCrosshair(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.options.getPerspective().isFirstPerson() || mc.options.hudHidden) return;
        int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
        int s = (int) size.value, g = (int) gap.value, t = (int) thickness.value, h = t / 2;
        bar(c, cx - h, cy - g - s, cx - h + t, cy - g);
        bar(c, cx - h, cy + g + (t % 2), cx - h + t, cy + g + s + (t % 2));
        bar(c, cx - g - s, cy - h, cx - g, cy - h + t);
        bar(c, cx + g + (t % 2), cy - h, cx + g + s + (t % 2), cy - h + t);
        if (dot.value) bar(c, cx - h, cy - h, cx - h + t, cy - h + t);
    }
}
