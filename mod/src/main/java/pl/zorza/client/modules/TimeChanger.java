package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.List;

/** Zmienia porę dnia tylko u ciebie (serwer i inni gracze tego nie widzą). */
public class TimeChanger extends Module {
    public final Setting.Choice time = add(new Setting.Choice("time", "Time", "Day",
            () -> List.of("Sunrise", "Day", "Noon", "Sunset", "Night", "Midnight")));
    public static long realTime = 0;

    public TimeChanger() {
        super("time_changer", "Time Changer", "Changes the time of day only for you.", Category.UTILITY, false);
    }

    public String icon() { return "m_clock"; }

    public long ticks() {
        return switch (time.value) {
            case "Sunrise" -> 23000;
            case "Noon" -> 6000;
            case "Sunset" -> 12500;
            case "Night" -> 15000;
            case "Midnight" -> 18000;
            default -> 1000;
        };
    }
}
