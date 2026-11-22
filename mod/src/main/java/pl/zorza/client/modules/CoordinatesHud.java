package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Setting;

public class CoordinatesHud extends HudModule {
    private final Setting.Bool biome = add(new Setting.Bool("biome", "Show biome", false));

    public CoordinatesHud() {
        super("coordinates", "Coordinates", "Your X / Y / Z position.", Category.HUD, true, 0.01, 0.10);
    }

    public String icon() { return "m_coords"; }

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
        for (String s : lines(mc)) w = Math.max(w, (int) R.width(s, 9));
        return w + 12;
    }

    public int height(MinecraftClient mc) { return lines(mc).length * 10 + 6; }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        int y = 4;
        for (String s : lines(mc)) {
            R.text(ctx, s, 6, y, 9, textColor.value);
            y += 10;
        }
    }
}
