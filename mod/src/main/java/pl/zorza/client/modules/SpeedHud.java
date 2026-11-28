package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class SpeedHud extends TextHud {
    private double lastX, lastZ, speed;

    public SpeedHud() {
        super("speed", "Speedometer", "Horizontal speed in blocks per second.", Category.HUD, false, 0.01, 0.57);
    }

    public String icon() { return "m_speed"; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        double dx = mc.player.getX() - lastX, dz = mc.player.getZ() - lastZ;
        speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
    }

    public String text(MinecraftClient mc) { return "Speed|" + String.format("%.2f", speed) + "|m/s"; }
}
