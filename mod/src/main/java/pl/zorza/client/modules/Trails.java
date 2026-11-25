package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.List;

public class Trails extends Module {
    public final Setting.Choice style = add(new Setting.Choice("style", "Style", "Glow line", () -> List.of("Glow line", "Ribbon")));
    public final Setting.Color color = add(new Setting.Color("color", "Color", 0xFF7C4DFF));
    public final Setting.Bool rainbow = add(new Setting.Bool("rainbow", "Rainbow", false));
    public final Setting.Num length = add(new Setting.Num("length2", "Length (seconds)", 2.6, 0.5, 6.0, 0.1));
    public final Setting.Num width = add(new Setting.Num("width2", "Thickness", 0.45, 0.1, 1.0, 0.01));
    public final Setting.Num height = add(new Setting.Num("height2", "Height (1.05 = back)", 1.05, 0.0, 1.8, 0.05));

    public Trails() {
        super("trails", "Trails", "A smooth glowing trail behind you.", Category.VISUAL, false);
    }

    public String icon() { return "m_trails"; }
}
