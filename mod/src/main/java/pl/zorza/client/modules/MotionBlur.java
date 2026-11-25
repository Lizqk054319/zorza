package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Lekkie rozmycie ruchu przy obracaniu kamerą (tylko świat, bez HUD-a). */
public class MotionBlur extends Module {
    public final Setting.Num strength = add(new Setting.Num("strength", "Strength %", 40, 0, 100, 5));

    public MotionBlur() {
        super("motion_blur", "Motion Blur", "Smooth blur when you move the camera. 0-100%.", Category.VISUAL, false);
    }

    public String icon() { return "m_motion_blur"; }
}
