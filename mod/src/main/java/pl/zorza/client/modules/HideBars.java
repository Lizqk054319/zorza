package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Chowa pasek zbroi nad sercami i/lub pasek głodu. */
public class HideBars extends Module {
    public final Setting.Bool armor = add(new Setting.Bool("armor", "Hide armor bar", true));
    public final Setting.Bool hunger = add(new Setting.Bool("hunger", "Hide hunger bar", false));

    public HideBars() {
        super("hide_bars", "Armor Bar", "Hides the armor bar above your hearts. Can also hide the hunger bar.", Category.VISUAL, false);
    }

    public boolean hideArmor() { return enabled && armor.value; }

    public boolean hideHunger() { return enabled && hunger.value; }

    public String icon() { return "m_armor"; }
}
