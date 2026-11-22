package pl.zorza.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import pl.zorza.client.modules.MenuSettings;

/** Menu po prawym Shifcie: logo, "Zorza Settings" i rząd ikon. */
public class QuickMenuScreen extends Screen {
    private final long opened = System.currentTimeMillis();
    private static final ItemStack[] ICONS = {new ItemStack(Items.COMPASS), new ItemStack(Items.WRITABLE_BOOK),
            new ItemStack(Items.NETHER_STAR), new ItemStack(Items.COMPARATOR)};
    private static final String[] TIPS = {"Edit HUD", "Mods", "Theme", "Game settings"};

    public QuickMenuScreen() {
        super(Text.literal("Zorza"));
    }

    @Override
    public boolean shouldPause() { return false; }

    private int cx() { return width / 2; }

    private int cy() { return height / 2; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        float a = MenuSettings.anim(opened, 320);
        int cx = cx(), cy = cy();
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy + (1 - a) * 14, 0);
        float s = 0.85f + 0.15f * a;
        ctx.getMatrices().scale(s, s, 1);
        ctx.getMatrices().translate(-cx, -cy, 0);

        R.logo(ctx, cx - 20, cy - 66, 40);
        boolean hov = R.in(mx, my, cx - 62, cy - 16, 124, 22);
        R.round(ctx, cx - 62, cy - 16, 124, 22, 5, hov ? R.mix(0xE0101014, R.ACCENT, 0.35f) : 0xE0101014);
        R.centered(ctx, "Zorza Settings", cx, cy - 9, R.alpha(0xFFFFFFFF, a));

        int x0 = cx - 53, y = cy + 14;
        for (int i = 0; i < 4; i++) {
            int x = x0 + i * 28;
            boolean h = R.in(mx, my, x, y, 22, 22);
            R.round(ctx, x, y, 22, 22, 5, h ? R.mix(0xE0101014, R.ACCENT, 0.35f) : 0xE0101014);
            ctx.drawItem(ICONS[i], x + 3, y + 3);
            if (h) R.centered(ctx, TIPS[i], x + 11, y + 28, 0xFFDDDDDD);
        }
        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int cx = cx(), cy = cy();
        if (R.in(mx, my, cx - 62, cy - 16, 124, 22)) {
            client.setScreen(new ModMenuScreen(null));
            return true;
        }
        int x0 = cx - 53, y = cy + 14;
        for (int i = 0; i < 4; i++) {
            if (R.in(mx, my, x0 + i * 28, y, 22, 22)) {
                switch (i) {
                    case 0 -> client.setScreen(new HudEditorScreen());
                    case 1 -> client.setScreen(new ModMenuScreen(null));
                    case 2 -> client.setScreen(new ModMenuScreen(MenuSettings.INSTANCE));
                    default -> client.setScreen(new OptionsScreen(this, client.options));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
