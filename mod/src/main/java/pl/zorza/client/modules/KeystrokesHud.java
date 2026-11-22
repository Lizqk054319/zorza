package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.gui.R;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Setting;

public class KeystrokesHud extends HudModule {
    private final Setting.Bool mouse = add(new Setting.Bool("mouse", "Show mouse buttons", true));
    private final Setting.Bool space = add(new Setting.Bool("space", "Show space bar", true));
    private final Setting.Bool cps = add(new Setting.Bool("cps", "Show CPS on mouse", true));
    private final Setting.Color pressed = add(new Setting.Color("pressed", "Pressed color", 0xFFFFFFFF));

    public KeystrokesHud() {
        super("keystrokes", "Keystrokes", "WASD, mouse and jump keys.", Category.HUD, false, 0.86, 0.02);
        background.value = false;
    }

    public ItemStack icon() { return new ItemStack(Items.OAK_BUTTON); }

    public int width(MinecraftClient mc) { return 70; }

    public int height(MinecraftClient mc) {
        return 46 + (mouse.value ? 24 : 0) + (space.value ? 14 : 0);
    }

    private void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, boolean down) {
        R.round(ctx, x, y, w, h, 3, down ? R.alpha(pressed.value, 0.85f) : 0x90101014);
        int col = down ? 0xFF000000 : textColor.value;
        String[] parts = label.split("\n");
        int ty = y + h / 2 - parts.length * 5 + 1;
        for (String p : parts) {
            ctx.drawText(mc.textRenderer, p, x + w / 2 - mc.textRenderer.getWidth(p) / 2, ty, col, shadow.value && !down);
            ty += 9;
        }
    }

    private static boolean d(KeyBinding k) { return k.isPressed(); }

    public void draw(DrawContext ctx, MinecraftClient mc, boolean editor) {
        var o = mc.options;
        key(ctx, mc, 24, 0, 22, 22, "W", d(o.forwardKey));
        key(ctx, mc, 0, 24, 22, 22, "A", d(o.leftKey));
        key(ctx, mc, 24, 24, 22, 22, "S", d(o.backKey));
        key(ctx, mc, 48, 24, 22, 22, "D", d(o.rightKey));
        int y = 48;
        if (mouse.value) {
            String l = cps.value ? "LMB\n" + ZorzaClient.leftCps() : "LMB";
            String r = cps.value ? "RMB\n" + ZorzaClient.rightCps() : "RMB";
            key(ctx, mc, 0, y, 34, 22, l, d(o.attackKey));
            key(ctx, mc, 36, y, 34, 22, r, d(o.useKey));
            y += 24;
        }
        if (space.value) key(ctx, mc, 0, y, 70, 12, "———", d(o.jumpKey));
    }
}
