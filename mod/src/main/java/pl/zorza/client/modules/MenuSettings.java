package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** Wygląd menu Zorzy: kolor akcentu i animacje. */
public class MenuSettings extends Module {
    public static MenuSettings INSTANCE;
    public final Setting.Color accent = add(new Setting.Color("accent", "Accent color", 0xFF2F6BFF));
    public final Setting.Bool animations = add(new Setting.Bool("animations", "Menu animations", true));
    public final Setting.Bool styledMinecraft = add(new Setting.Bool("styled_minecraft", "Zora style for Minecraft menus", true));
    public final Setting.Bool customHotbar = add(new Setting.Bool("custom_hotbar", "Custom hotbar", true));

    public static boolean hotbar() { return INSTANCE == null || INSTANCE.customHotbar.value; }

    public static boolean styled() { return INSTANCE == null || INSTANCE.styledMinecraft.value; }

    public MenuSettings() {
        super("menu", "Zora Menu", "Accent color and animations of this menu.", Category.CLIENT, true);
        INSTANCE = this;
    }

    public String icon() { return "m_palette"; }

    @Override
    public void onTick(MinecraftClient mc) { R.ACCENT = accent.value; }

    public static float anim(long start, float ms) {
        if (INSTANCE != null && !INSTANCE.animations.value) return 1f;
        return R.ease(start, ms);
    }
}
