package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Rozjaśnia świat w ciemnych miejscach. 0% = zwykła maksymalna jasność gry, 150% = prawie jak w dzień. */
public class Gamma extends Module {
    private final Setting.Num brightness = add(new Setting.Num("brightness", "Brightness %", 100, 0, 150, 5));

    public Gamma() {
        super("gamma", "Gamma", "Brightens dark places. 0% = normal max brightness, 150% = almost like daytime.", Category.UTILITY, false);
    }

    public String icon() { return "m_sun"; }

    /** Wartość jasności przekazywana do oświetlenia (gra normalnie używa 0..1). */
    public Double factor() {
        return 1.0 + brightness.value / 100.0 * 9.5;      // 150% ≈ 15 – cienie też się rozjaśniają
    }
}
