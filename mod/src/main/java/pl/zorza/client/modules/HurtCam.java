package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Ustawia siłę bujania kamery przy obrażeniach (opcja z gry, 0 = wyłączone). */
public class HurtCam extends Module {
    private final Setting.Num strength = add(new Setting.Num("strength", "Shake strength", 0.0, 0.0, 1.0, 0.05));

    public HurtCam() {
        super("hurt_cam", "Hurt Cam", "Reduce or remove camera shake when hurt.", Category.COMBAT, false);
    }

    public String icon() { return "m_camera"; }

    @Override
    public void onTick(MinecraftClient mc) {
        var opt = mc.options.getDamageTiltStrength();
        if (Math.abs(opt.getValue() - strength.value) > 0.001) opt.setValue(strength.value);
    }

    @Override
    public void onDisable() {
        MinecraftClient.getInstance().options.getDamageTiltStrength().setValue(1.0);
    }
}
