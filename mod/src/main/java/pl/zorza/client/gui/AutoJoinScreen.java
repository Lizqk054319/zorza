package pl.zorza.client.gui;

import pl.zorza.client.compat.Ids;

import pl.zorza.client.compat.Gfx;
import pl.zorza.client.compat.M;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
//#if MC >= 1.20.2
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
//#else
//$$ import net.minecraft.client.gui.screen.ConnectScreen;
//#endif
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import pl.zorza.client.AutoJoin;

import java.text.NumberFormat;
import java.util.Locale;

/** "Połączyć z serwerem?" – pokazywane po uruchomieniu gry z zakładki Serwery w launcherze. */
public class AutoJoinScreen extends ZScreen {
    private static final Identifier ICON = Ids.of("zorza", "autojoin_icon");
    private final Screen parent;
    private final AutoJoin.Data data;
    private boolean hasIcon = false;
    private final boolean pl;
    private int cx, cy, cw, ch;

    public AutoJoinScreen(Screen parent, AutoJoin.Data data) {
        super("Zora Client");
        this.parent = parent;
        this.data = data;
        this.pl = MinecraftClient.getInstance().getLanguageManager().getLanguage().startsWith("pl");
        if (data.favicon() != null) {
            try {
                NativeImage img = NativeImage.read(data.favicon());
                MinecraftClient.getInstance().getTextureManager().registerTexture(ICON, Gfx.nativeTexture(img));
                hasIcon = true;
            } catch (Exception ignored) {
            }
        }
    }

    private String s(String plText, String en) { return pl ? plText : en; }

    private void layout() {
        cw = 860;
        ch = 380;
        cx = (W - cw) / 2;
        cy = (H - ch) / 2;
    }

    private int[] yesRect() { return new int[]{cx + cw - 32 - 220 - 14 - 160, cy + ch - 32 - 56, 220, 56}; }

    private int[] noRect() { return new int[]{cx + cw - 32 - 160, cy + ch - 32 - 56, 160, 56}; }

    @Override
    protected void draw(DrawContext ctx, double mx, double my, float delta) {
        layout();
        float a = R.ease(opened, 320);
        int off = (int) ((1 - a) * 24);
        int y0 = cy + off;
        R.round(ctx, cx, y0, cw, ch, 22, R.alpha(0xF0141418, a));
        R.logo(ctx, cx + 32, y0 + 30, 34, R.alpha(0xFFFFFFFF, a));
        R.text(ctx, "Zora Client", cx + 76, R.midY(y0 + 30, 34, 16), 16, R.alpha(0xFF9C9CA6, a), R.SEMI);
        R.text(ctx, s("Połączyć z serwerem?", "Connect to the server?"), cx + 32, y0 + 84, 32, R.alpha(0xFFFFFFFF, a), R.BOLD);

        // wiersz serwera – jak na liście serwerów
        int ry = y0 + 140, rh = 104;
        R.round(ctx, cx + 32, ry, cw - 64, rh, 16, R.alpha(0xFF1C1C22, a));
        int ix = cx + 48, iy = ry + 16;
        if (hasIcon) R.tex(ctx, ICON, ix, iy, 72, 72, R.alpha(0xFFFFFFFF, a));
        else R.logo(ctx, ix + 8, iy + 8, 56, R.alpha(0xFFFFFFFF, a));
        int tx = ix + 90;
        R.text(ctx, data.name(), tx, iy + 2, 18, R.alpha(0xFFFFFFFF, a), R.BOLD);
        if (data.max() >= 0) {
            NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ROOT);
            String p = nf.format(Math.max(0, data.players())) + "/" + nf.format(data.max());
            R.text(ctx, p, cx + cw - 48 - R.width(p, 16, R.SEMI), iy + 3, 16, R.alpha(0xFFB8B8C2, a), R.SEMI);
        }
        int ly = iy + 30;
        for (int i = 0; i < Math.min(2, data.motd().size()); i++) {
            M.push(ctx);
            M.translate(ctx, tx, ly + i * 20);
            M.scale(ctx, 2f, 2f);
            ctx.drawText(textRenderer, data.motd().get(i), 0, 0, 0xFFAAAAAA, false);
            M.pop(ctx);
        }
        R.text(ctx, s("Adres: ", "Address: ") + data.address(), cx + 32, ry + rh + 18, 15, R.alpha(0xFF8C8C96, a), R.MEDIUM);

        int[] y = yesRect(), n = noRect();
        y[1] += off;
        n[1] += off;
        float hy = hover("yes", R.in(mx, my, y[0], y[1], y[2], y[3])), hn = hover("no", R.in(mx, my, n[0], n[1], n[2], n[3]));
        R.round(ctx, y[0], y[1], y[2], y[3], 14, R.alpha(R.mix(R.ACCENT, 0xFFFFFFFF, hy * 0.15f), a));
        R.centered(ctx, s("Tak, połącz", "Yes, connect"), y[0] + y[2] / 2f, R.midY(y[1], y[3], 17), 17, R.alpha(R.onAccent(), a), R.SEMI);
        R.round(ctx, n[0], n[1], n[2], n[3], 14, R.alpha(R.mix(0xFF26262E, 0xFF32323C, hn), a));
        R.centered(ctx, s("Nie", "No"), n[0] + n[2] / 2f, R.midY(n[1], n[3], 17), 17, R.alpha(0xFFFFFFFF, a), R.SEMI);
    }

    @Override
    protected boolean click(double mx, double my, int button) {
        layout();
        int[] y = yesRect(), n = noRect();
        if (R.in(mx, my, y[0], y[1], y[2], y[3])) {
            connect();
            return true;
        }
        if (R.in(mx, my, n[0], n[1], n[2], n[3])) {
            close();
            return true;
        }
        return true;
    }

    @Override
    protected boolean key(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            connect();
            return true;
        }
        return false;
    }

    private void connect() {
        //#if MC >= 1.20.2
        ServerInfo info = new ServerInfo(data.name(), data.address(), ServerInfo.ServerType.OTHER);
        //#else
        //$$ ServerInfo info = new ServerInfo(data.name(), data.address(), false);
        //#endif
        //#if MC >= 1.20.5
        ConnectScreen.connect(parent, client, ServerAddress.parse(data.address()), info, false, null);
        //#else
        //$$ ConnectScreen.connect(parent, client, ServerAddress.parse(data.address()), info, false);
        //#endif
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        if (hasIcon) client.getTextureManager().destroyTexture(ICON);
    }
}
