package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

public class LowFire extends Module {
    public final Setting.Num height = add(new Setting.Num("height", "Lower by", 0.35, 0.0, 0.6, 0.05));

    public LowFire() {
        super("low_fire", "Low Fire", "Moves the fire overlay down so you can see while burning.", Category.UTILITY, false);
    }

    public String icon() { return "m_potion"; }
}
