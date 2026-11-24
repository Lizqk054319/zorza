package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.Modules;
import pl.zorza.client.module.TextHud;

public class DayCounterHud extends TextHud {
    public DayCounterHud() {
        super("day_counter", "Day Counter", "Shows which in-game day it is.", Category.HUD, false, 0.01, 0.40);
    }

    public String icon() { return "m_clock"; }

    public String text(MinecraftClient mc) {
        if (mc.world == null) return "Day|1";
        long t = Modules.TIME_CHANGER.enabled ? TimeChanger.realTime : mc.world.getTimeOfDay();
        return "Day|" + (t / 24000L + 1);
    }
}
