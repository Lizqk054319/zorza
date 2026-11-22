package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

public class Zoom extends Module {
    public final Setting.Num factor = add(new Setting.Num("factor", "Zoom amount", 4.0, 2.0, 10.0, 0.5));
    public final Setting.Bool smooth = add(new Setting.Bool("smooth", "Smooth zoom", true));

    public Zoom() {
        super("zoom", "Zoom", "Hold C to zoom in (key in Controls).", Category.UTILITY, true);
    }

    public String icon() { return "m_zoom"; }
}
