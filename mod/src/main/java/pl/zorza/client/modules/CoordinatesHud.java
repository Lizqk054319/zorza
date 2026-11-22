package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Setting;

public class CoordinatesHud extends HudModule {
    private final Setting.Bool biome = add(new Setting.Bool("biome", "Show biome", false));

    public CoordinatesHud() {
        super("coordinates", "Coordinates", "Your X / Y / Z position.", Category.HUD, true, 0.01, 0.10);
    }

    public ItemStack icon() { return new ItemStack(Items.COMPASS); }

    private String[] lines(MinecraftClient mc) {
        if (mc.player == null) return new String[]{"X: 0", "Y: 64", "Z: 0"};
        var p = mc.player.getBlockPos();
        if (biome.value && mc.world != null) {
            String b = mc.world.getBiome(p).getKey().map(k -> k.getValue().getPath()).orElse("?");
            return new String[]{"X: " + p.getX(), "Y: " + p.getY(), "Z: " + p.getZ(), "Biome: " + b};
        }
        return new String[]{"X: " + p.getX(), "Y: " + p.getY(), "Z: " + p.getZ()};
    }

    public int width(MinecraftClient mc) {
        int w = 0;
        for (String s : lines(mc)) w = Math.max(w, mc.textRenderer.getWidth(s));
        return w + 12;
    }

    public int height(MinecraftClient mc) { return lines(mc).length * 10 + 6; }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int y = 4;
        for (String s : lines(mc)) {
            ctx.drawText(mc.textRenderer, s, 6, y, textColor.value, shadow.value);
            y += 10;
        }
    }
}
