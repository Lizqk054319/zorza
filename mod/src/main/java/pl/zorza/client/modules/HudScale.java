package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Rozmiar listy graczy (Tab), scoreboardu i paska bossa. Zawsze aktywne – 1.0 = jak w Minecrafcie. */
public class HudScale extends Module {
    public final Setting.Num tab = add(new Setting.Num("tab", "Tab list size", 1.0, 0.5, 2.0, 0.05));
    public final Setting.Num scoreboard = add(new Setting.Num("scoreboard", "Scoreboard size", 1.0, 0.5, 2.0, 0.05));
    public final Setting.Num bossbar = add(new Setting.Num("bossbar", "Boss bar size", 1.0, 0.5, 2.0, 0.05));

    public HudScale() {
        super("hud_scale", "Tab & Scoreboard", "Size of the tab list, scoreboard and boss bar.", Category.CLIENT, true);
    }

    public String icon() { return "m_resize"; }

    public float tab() { return enabled ? (float) tab.value : 1f; }

    public float scoreboard() { return enabled ? (float) scoreboard.value : 1f; }

    public float bossbar() { return enabled ? (float) bossbar.value : 1f; }
}
