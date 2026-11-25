package pl.zorza.client.modules;

import pl.zorza.client.fx.Effects;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

public class AmbientParticles extends Module {
    public final Setting.Choice shape = add(new Setting.Choice("shape", "Particles", "Snow", () -> Effects.SHAPES));
    public final Setting.Color color = add(new Setting.Color("color", "Color", 0xFFFFFFFF));
    public final Setting.Bool rainbow = add(new Setting.Bool("rainbow", "Rainbow", false));
    public final Setting.Num density = add(new Setting.Num("density", "Density", 1.0, 0.2, 4.0, 0.1));
    public final Setting.Num size = add(new Setting.Num("size", "Size", 1.0, 0.4, 2.5, 0.1));

    public AmbientParticles() {
        super("ambient_particles", "Ambient Particles", "Custom snowflakes, stars or hearts falling around you.", Category.VISUAL, false);
    }

    public String icon() { return "m_ambient"; }
}
