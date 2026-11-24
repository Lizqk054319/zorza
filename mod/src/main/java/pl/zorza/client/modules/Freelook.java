package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.List;

/** Freelook (jak w Lunar Client): kamera obraca się wokół postaci, a postać dalej patrzy tam, gdzie patrzyła. */
public class Freelook extends Module {
    public final Setting.Choice mode = add(new Setting.Choice("mode", "Mode", "Hold", () -> List.of("Hold", "Toggle")));
    public final Setting.Bool thirdPerson = add(new Setting.Bool("third_person", "Third person view", true));
    public final Setting.Bool full360 = add(new Setting.Bool("full_360", "360° rotation (also up/down)", true));
    public final Setting.Bool invert = add(new Setting.Bool("invert", "Invert vertical", false));
    public final Setting.Bool invertX = add(new Setting.Bool("invert_x", "Invert horizontal", false));
    public final Setting.Num sensitivity = add(new Setting.Num("sensitivity", "Sensitivity", 1.0, 0.2, 3.0, 0.1));

    public boolean active = false;
    public float yaw, pitch;
    private boolean toggled = false;
    private Perspective previous;

    public Freelook() {
        super("freelook", "Freelook", "Look around your character without turning. Default key: Left Alt (change in Controls).", Category.UTILITY, true);
    }

    public String icon() { return "m_camera"; }

    /** Co tick (także gdy moduł wyłączony – żeby zawsze dało się wyjść z freelooka). */
    public void update(MinecraftClient mc) {
        boolean want;
        if ("Toggle".equals(mode.value)) {
            while (ZorzaClient.FREELOOK_KEY.wasPressed()) toggled = !toggled;
            want = toggled;
        } else {
            while (ZorzaClient.FREELOOK_KEY.wasPressed()) { }
            toggled = false;
            want = ZorzaClient.FREELOOK_KEY.isPressed() && mc.currentScreen == null;
        }
        want &= enabled && mc.player != null && mc.world != null;
        if (want && !active) start(mc);
        else if (!want && active) stop(mc);
    }

    private void start(MinecraftClient mc) {
        active = true;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        previous = mc.options.getPerspective();
        if (thirdPerson.value && previous.isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    private void stop(MinecraftClient mc) {
        active = false;
        toggled = false;
        if (previous != null) mc.options.setPerspective(previous);
        previous = null;
    }

    /** Ruch myszy – zwraca true, gdy przejęliśmy go dla kamery. */
    public boolean onMouse(double dx, double dy) {
        if (!active) return false;
        double s = 0.15 * sensitivity.value;
        yaw = MathHelper.wrapDegrees(yaw + (float) (dx * s * (invertX.value ? -1 : 1)));
        float p = pitch + (float) (dy * s * (invert.value ? -1 : 1));
        pitch = full360.value ? MathHelper.wrapDegrees(p) : MathHelper.clamp(p, -90f, 90f);
        return true;
    }
}
