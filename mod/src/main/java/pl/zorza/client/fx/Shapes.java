package pl.zorza.client.fx;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/** Proste kształty z kolorowych czworokątów (bez tekstur) – rysowane w płaszczyźnie XY macierzy. */
public final class Shapes {
    private Shapes() {}

    public static void tri(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        vc.vertex(m, x1, y1, 0).color(r, g, b, a);
        vc.vertex(m, x2, y2, 0).color(r, g, b, a);
        vc.vertex(m, x3, y3, 0).color(r, g, b, a);
        vc.vertex(m, x3, y3, 0).color(r, g, b, a);
    }

    public static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int argb) {
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        vc.vertex(m, x1, y1, 0).color(r, g, b, a);
        vc.vertex(m, x2, y2, 0).color(r, g, b, a);
        vc.vertex(m, x3, y3, 0).color(r, g, b, a);
        vc.vertex(m, x4, y4, 0).color(r, g, b, a);
    }

    /** Wielokąt z punktów (wachlarz od środka). */
    public static void fan(VertexConsumer vc, Matrix4f m, float[] xs, float[] ys, int center, int edge) {
        int n = xs.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            int a1 = center >>> 24, a2 = edge >>> 24;
            vc.vertex(m, 0, 0, 0).color((center >> 16) & 255, (center >> 8) & 255, center & 255, a1);
            vc.vertex(m, xs[i], ys[i], 0).color((edge >> 16) & 255, (edge >> 8) & 255, edge & 255, a2);
            vc.vertex(m, xs[j], ys[j], 0).color((edge >> 16) & 255, (edge >> 8) & 255, edge & 255, a2);
            vc.vertex(m, xs[j], ys[j], 0).color((edge >> 16) & 255, (edge >> 8) & 255, edge & 255, a2);
        }
    }

    public static void star(VertexConsumer vc, Matrix4f m, int points, float inner, int color) {
        int n = points * 2;
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            double ang = Math.PI / 2 + i * Math.PI / points;
            float r = i % 2 == 0 ? 1f : inner;
            xs[i] = (float) Math.cos(ang) * r;
            ys[i] = (float) Math.sin(ang) * r;
        }
        fan(vc, m, xs, ys, color, color);
    }

    public static void circle(VertexConsumer vc, Matrix4f m, int seg, int center, int edge) {
        float[] xs = new float[seg], ys = new float[seg];
        for (int i = 0; i < seg; i++) {
            double ang = i * Math.PI * 2 / seg;
            xs[i] = (float) Math.cos(ang);
            ys[i] = (float) Math.sin(ang);
        }
        fan(vc, m, xs, ys, center, edge);
    }

    public static void heart(VertexConsumer vc, Matrix4f m, int color) {
        int seg = 28;
        float[] xs = new float[seg], ys = new float[seg];
        for (int i = 0; i < seg; i++) {
            double t = i * Math.PI * 2 / seg;
            xs[i] = (float) (16 * Math.pow(Math.sin(t), 3)) / 17f;
            ys[i] = (float) (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 17f + 0.1f;
        }
        fan(vc, m, xs, ys, color, color);
    }

    /** Śnieżynka: 3 ramiona z odgałęzieniami. */
    public static void snowflake(VertexConsumer vc, Matrix4f m, int color) {
        float w = 0.09f;
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI / 3;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            line(vc, m, -c, -s, c, s, w, color);
            for (int side = -1; side <= 1; side += 2) {               // małe gałązki na obu końcach
                float bx = c * 0.55f * side, by = s * 0.55f * side;
                double b1 = a + (side > 0 ? 0 : Math.PI);
                for (int d = -1; d <= 1; d += 2) {
                    double ang = b1 + d * Math.PI / 4;
                    line(vc, m, bx, by, bx + (float) Math.cos(ang) * 0.35f, by + (float) Math.sin(ang) * 0.35f, w * 0.8f, color);
                }
            }
        }
    }

    public static void line(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float w, int color) {
        float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-5f) return;
        float nx = -dy / len * w / 2, ny = dx / len * w / 2;
        quad(vc, m, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, color);
    }

    /** Obrys kształtu (ring) – punkty na jednostkowym konturze, grubość t. */
    public static void ring(VertexConsumer vc, Matrix4f m, float[] xs, float[] ys, float r, float t, int color) {
        int n = xs.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ro = r + t / 2, ri = Math.max(0, r - t / 2);
            quad(vc, m, xs[i] * ri, ys[i] * ri, xs[i] * ro, ys[i] * ro, xs[j] * ro, ys[j] * ro, xs[j] * ri, ys[j] * ri, color);
        }
    }

    public static int alpha(int argb, float a) {
        int na = Math.max(0, Math.min(255, Math.round((argb >>> 24) * a)));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    public static int rainbow(float offset) {
        float h = (System.currentTimeMillis() % 4000L) / 4000f + offset;
        return 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(h - (float) Math.floor(h), 0.65f, 1f);
    }
}
