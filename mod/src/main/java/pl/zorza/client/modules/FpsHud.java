package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class FpsHud extends TextHud {
    public FpsHud() {
        super("fps", "FPS", "Shows your frames per second.", Category.HUD, true, 0.01, 0.02);
    }

    public String icon() { return "m_fps"; }

    public String text(MinecraftClient mc) { return "FPS|" + mc.getCurrentFps(); }
}
