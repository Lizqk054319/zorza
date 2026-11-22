package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Setting;
import pl.zorza.client.module.TextHud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockHud extends TextHud {
    private final Setting.Bool h12 = add(new Setting.Bool("h12", "12-hour format", false));

    public ClockHud() {
        super("clock", "Clock", "Real-world time.", Category.HUD, false, 0.01, 0.20);
    }

    public String icon() { return "m_clock"; }

    public String text(MinecraftClient mc) {
        return LocalTime.now().format(DateTimeFormatter.ofPattern(h12.value ? "hh:mm a" : "HH:mm"));
    }
}
