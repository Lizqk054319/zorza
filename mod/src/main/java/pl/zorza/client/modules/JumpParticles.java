package pl.zorza.client.modules;

import pl.zorza.client.fx.Effects;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

public class JumpParticles extends Module {
    public final Setting.Choice shape = add(new Setting.Choice("shape", "Particles", "Stars", () -> Effects.SHAPES));
    public final Setting.Color color = add(new Setting.Color("color", "Color", 0xFFF5B400));
    public final Setting.Bool rainbow = add(new Setting.Bool("rainbow", "Rainbow", false));
    public final Setting.Num amount = add(new Setting.Num("amount", "Amount", 14, 4, 40, 1));
    public final Setting.Num size = add(new Setting.Num("size", "Size", 1.0, 0.4, 2.5, 0.1));

    public JumpParticles() {
        super("jump_particles", "Jump Particles", "Custom particles (stars, snow, hearts...) burst out when you jump.", Category.VISUAL, false);
    }

    public String icon() { return "m_jump_particles"; }
}
