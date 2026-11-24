package pl.zorza.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Rysowanie w stylu Zorzy: gładkie zaokrąglone rogi (tekstury z antyaliasingiem),
 * czcionka Inter zamiast pikselowej i wektorowe ikonki.
 */
public final class R {
    public static final Identifier LOGO = Identifier.of("zorza", "textures/gui/logo.png");
    /** Grubość czcionki – właściwy plik (z odpowiednią rozdzielczością) wybiera R.font(). */
    public static final Identifier MEDIUM = Identifier.of("zorza", "inter_medium");
    public static final Identifier SEMI = Identifier.of("zorza", "inter_semibold");
    public static final Identifier BOLD = Identifier.of("zorza", "inter_bold");
    private static final int[] OVERSAMPLE = {2, 3, 4, 6, 8};
    /** Ile prawdziwych pikseli przypada na jednostkę, w której teraz rysujemy (ustawia ekran / HUD). */
    public static float pxPerUnit = 2f;
    private static final Identifier TL = Identifier.of("zorza", "textures/gui/corner_tl.png");
    private static final Identifier TR = Identifier.of("zorza", "textures/gui/corner_tr.png");
    private static final Identifier BL = Identifier.of("zorza", "textures/gui/corner_bl.png");
    private static final Identifier BR = Identifier.of("zorza", "textures/gui/corner_br.png");

    public static int ACCENT = 0xFF2F6BFF;

    /** Kolor tekstu/ikon na tle w kolorze akcentu – ciemny, gdy akcent jest jasny (np. biały). */
    public static int onAccent() {
        int r = (ACCENT >> 16) & 255, g = (ACCENT >> 8) & 255, b = ACCENT & 255;
        return (0.299 * r + 0.587 * g + 0.114 * b) > 170 ? 0xFF111116 : 0xFFFFFFFF;
    }

    /** Czy akcent jest jasny. */
    public static boolean lightAccent() { return onAccent() != 0xFFFFFFFF; }
    public static final int PANEL = 0xEB121216;
    public static final int CARD = 0xFF1A1A1F;
    public static final int CARD_HOVER = 0xFF222228;
    public static final int BUTTON = 0xF0121215;
    public static final int BUTTON_HOVER = 0xF01E1E24;
    public static final int MUTED = 0xFF8C8C96;
    public static final int GREEN = 0xFF1E6B2C;
    public static final int GREEN_TEXT = 0xFFB4F5C2;

    private R() {}

    // ---------------------------------------------------------------- kształty

    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x + r, y, x + w - r, y + h, color);
        c.fill(x, y + r, x + r, y + h - r, color);
        c.fill(x + w - r, y + r, x + w, y + h - r, color);
        tex(c, TL, x, y, r, r, color);
        tex(c, TR, x + w - r, y, r, r, color);
        tex(c, BL, x, y + h - r, r, r, color);
        tex(c, BR, x + w - r, y + h - r, r, r, color);
    }

    /** Zaokrąglony prostokąt z obramowaniem o grubości b. */
    public static void bordered(DrawContext c, int x, int y, int w, int h, int r, int b, int fill, int border) {
        round(c, x, y, w, h, r, border);
        round(c, x + b, y + b, w - 2 * b, h - 2 * b, Math.max(0, r - b), fill);
    }

    public static void tex(DrawContext c, Identifier id, int x, int y, int w, int h, int color) {
        c.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0f, 0f, w, h, w, h, color);
    }

    public static void icon(DrawContext c, String name, int x, int y, int size, int color) {
        tex(c, Identifier.of("zorza", "textures/gui/icons/" + name + ".png"), x, y, size, size, color);
    }

    public static void logo(DrawContext c, int x, int y, int size, int color) {
        tex(c, LOGO, x, y, size, size, color);
    }

    // ---------------------------------------------------------------- tekst (Inter)

    public static Text styled(String s, Identifier font) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(font));
    }

    /** Wybiera wariant czcionki rasteryzowany mniej więcej w docelowym rozmiarze – bez "zjadania" liter. */
    private static Identifier font(Identifier weight, float size) {
        float need = size * pxPerUnit / 11f;
        int pick = OVERSAMPLE[OVERSAMPLE.length - 1];
        for (int o : OVERSAMPLE) {
            if (o >= need * 0.85f) {
                pick = o;
                break;
            }
        }
        return Identifier.of("zorza", weight.getPath() + "_" + pick);
    }

    /** Tekst o wysokości ~size jednostek; (x, y) to lewy górny róg. */
    public static void text(DrawContext c, String s, float x, float y, float size, int color, Identifier font) {
        if ((color >>> 24) < 5) return;
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        float k = size / 11f;
        m.scale(k, k, 1f);
        c.drawText(MinecraftClient.getInstance().textRenderer, styled(s, font(font, size)), 0, 0, color, false);
        m.pop();
    }

    public static void text(DrawContext c, String s, float x, float y, float size, int color) {
        text(c, s, x, y, size, color, MEDIUM);
    }

    public static void shadowText(DrawContext c, String s, float x, float y, float size, int color, Identifier font) {
        float o = Math.max(1f / Math.max(0.5f, pxPerUnit), size * 0.045f);      // ~1 prawdziwy piksel
        text(c, s, x + o, y + o, size, alpha(0xFF000000, ((color >>> 24) / 255f) * 0.4f), font);
        text(c, s, x, y, size, color, font);
    }

    public static float width(String s, float size, Identifier font) {
        return MinecraftClient.getInstance().textRenderer.getWidth(styled(s, Identifier.of("zorza", font.getPath() + "_4"))) * size / 11f;
    }

    public static float width(String s, float size) {
        return width(s, size, MEDIUM);
    }

    public static void centered(DrawContext c, String s, float cx, float y, float size, int color, Identifier font) {
        text(c, s, cx - width(s, size, font) / 2f, y, size, color, font);
    }

    /** Wyśrodkowanie w pionie w polu o wysokości h. */
    public static float midY(float y, float h, float size) {
        return y + (h - size * 0.82f) / 2f;
    }

    // ---------------------------------------------------------------- kolory i animacje

    public static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** 0 -> 1 z łagodnym wyhamowaniem. */
    public static float ease(long start, float ms) {
        float t = Math.max(0f, Math.min(1f, (System.currentTimeMillis() - start) / ms));
        return 1f - (float) Math.pow(1f - t, 3);
    }

    /** Płynne zbliżanie wartości (do animacji hover). */
    public static float approach(float cur, float target, float speed) {
        return cur + (target - cur) * Math.min(1f, speed);
    }
}
