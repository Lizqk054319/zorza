package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Hitboxy jak F3+B, ale z własnym kolorem i bez czerwonej linii przy głowie. */
public class CustomHitboxes extends Module {
    public final Setting.Color color = add(new Setting.Color("color", "Color", 0xFFFFFFFF));
    public final Setting.Num opacity = add(new Setting.Num("opacity", "Opacity %", 100, 10, 100, 5));
    public final Setting.Bool lookLine = add(new Setting.Bool("look_line", "Show look direction (blue line)", false));
    public final Setting.Bool playersOnly = add(new Setting.Bool("players_only", "Only players", false));
    private boolean turnedOn = false;

    public CustomHitboxes() {
        super("custom_hitboxes", "Custom Hitboxes", "Shows hitboxes in your own color, without the red eye line.", Category.COMBAT, false);
    }

    public String icon() { return "m_hitbox"; }

    //#if MC < 1.21.9
    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.getEntityRenderDispatcher() == null) return;
        if (!mc.getEntityRenderDispatcher().shouldRenderHitboxes()) {
            mc.getEntityRenderDispatcher().setRenderHitboxes(true);
            turnedOn = true;
        }
    }

    /** Przy wyłączeniu chowamy hitboxy, jeśli to my je włączyliśmy. */
    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (turnedOn && mc.getEntityRenderDispatcher() != null) mc.getEntityRenderDispatcher().setRenderHitboxes(false);
        turnedOn = false;
    }
    //#endif
}
