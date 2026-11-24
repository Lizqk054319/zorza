package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;

public class ClearWeather extends Module {
    public ClearWeather() {
        super("clear_weather", "Clear Weather", "Hides rain, snow and thunderstorms (only for you).", Category.UTILITY, false);
    }

    public String icon() { return "m_sun"; }
}
