package pl.zorza.client.fx;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/** Proste kształty z kolorowych czworokątów (bez tekstur) – rysowane w płaszczyźnie XY macierzy. */
public final class Shapes {
    private Shapes() {}

    /** Czworokąt widoczny z obu stron (2 kolejności wierzchołków). */
    public static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int argb) {
        quadC(vc, m, x1, y1, argb, x2, y2, argb, x3, y3, argb, x4, y4, argb);
    }

    public static void quadC(VertexConsumer vc, Matrix4f m, float x1, float y1, int c1, float x2, float y2, int c2,
                             float x3, float y3, int c3, float x4, float y4, int c4) {
        v(vc, m, x1, y1, c1); v(vc, m, x2, y2, c2); v(vc, m, x3, y3, c3); v(vc, m, x4, y4, c4);
        v(vc, m, x4, y4, c4); v(vc, m, x3, y3, c3); v(vc, m, x2, y2, c2); v(vc, m, x1, y1, c1);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, int c) {
        //#if MC >= 1.21
        vc.vertex(m, x, y, 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, c >>> 24);
        //#else
        //$$ vc.vertex(m, x, y, 0).color((c >> 16) & 255, (c >> 8) & 255, c & 255, c >>> 24).next();
        //#endif
    }

    /** Wielokąt z punktów (wachlarz od środka). */
    public static void fan(VertexConsumer vc, Matrix4f m, float[] xs, float[] ys, int center, int edge) {
        int n = xs.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            quadC(vc, m, 0, 0, center, xs[i], ys[i], edge, xs[j], ys[j], edge, xs[j], ys[j], edge);
        }
    }

    private static final java.util.Map<Long, float[][]> CACHE = new java.util.HashMap<>();

    private static float[][] cached(long key, java.util.function.Supplier<float[][]> make) {
        float[][] r = CACHE.get(key);
        if (r == null) CACHE.put(key, r = make.get());
        return r;
    }

    public static void star(VertexConsumer vc, Matrix4f m, int points, float inner, int color) {
        float[][] p = cached(((long) points << 32) | Float.floatToIntBits(inner), () -> starPts(points, inner));
        fan(vc, m, p[0], p[1], color, color);
    }

    private static float[][] starPts(int points, float inner) {
        int n = points * 2;
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            double ang = Math.PI / 2 + i * Math.PI / points;
            float r = i % 2 == 0 ? 1f : inner;
            xs[i] = (float) Math.cos(ang) * r;
            ys[i] = (float) Math.sin(ang) * r;
        }
        return new float[][]{xs, ys};
    }

    public static void circle(VertexConsumer vc, Matrix4f m, int seg, int center, int edge) {
        float[][] p = cached(-1000L - seg, () -> circlePts(seg));
        fan(vc, m, p[0], p[1], center, edge);
    }

    private static float[][] circlePts(int seg) {
        float[] xs = new float[seg], ys = new float[seg];
        for (int i = 0; i < seg; i++) {
            double ang = i * Math.PI * 2 / seg;
            xs[i] = (float) Math.cos(ang);
            ys[i] = (float) Math.sin(ang);
        }
        return new float[][]{xs, ys};
    }

    public static void heart(VertexConsumer vc, Matrix4f m, int color) {
        float[][] p = cached(-1L, Shapes::heartPts);
        fan(vc, m, p[0], p[1], color, color);
    }

    private static float[][] heartPts() {
        int seg = 28;
        float[] xs = new float[seg], ys = new float[seg];
        for (int i = 0; i < seg; i++) {
            double t = i * Math.PI * 2 / seg;
            xs[i] = (float) (16 * Math.pow(Math.sin(t), 3)) / 17f;
            ys[i] = (float) (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 17f + 0.1f;
        }
        return new float[][]{xs, ys};
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
