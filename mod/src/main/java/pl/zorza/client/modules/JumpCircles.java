package pl.zorza.client.modules;

import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.List;

public class JumpCircles extends Module {
    public final Setting.Choice shape = add(new Setting.Choice("shape", "Shape", "Circle",
            () -> List.of("Circle", "Double ring", "Square", "Hexagon", "Star", "Heart", "Diamond")));
    public final Setting.Color color = add(new Setting.Color("color", "Color", 0xFF06B6D4));
    public final Setting.Bool rainbow = add(new Setting.Bool("rainbow", "Rainbow", false));
    public final Setting.Num radius = add(new Setting.Num("radius", "Size", 1.3, 0.4, 3.0, 0.1));
    public final Setting.Num duration = add(new Setting.Num("duration", "Duration (seconds)", 0.9, 0.3, 2.5, 0.1));
    public final Setting.Num spin = add(new Setting.Num("spin", "Spin", 90, 0, 360, 15));

    public JumpCircles() {
        super("jump_circles", "Jump Circles", "A glowing shape appears under you when you jump.", Category.VISUAL, false);
    }

    public String icon() { return "m_jump_circles"; }

    /** Kontur kształtu w jednostkach (promień 1). */
    public static float[][] outline(String shape) {
        int n;
        float[] r;
        switch (shape) {
            case "Square" -> { n = 4; r = null; return poly(4, 45, 1.2f); }
            case "Hexagon" -> { return poly(6, 0, 1f); }
            case "Diamond" -> { return poly(4, 0, 1.15f); }
            case "Star" -> {
                n = 10;
                float[] xs = new float[n], ys = new float[n];
                for (int i = 0; i < n; i++) {
                    double a = Math.PI / 2 + i * Math.PI / 5;
                    float rr = i % 2 == 0 ? 1.1f : 0.5f;
                    xs[i] = (float) Math.cos(a) * rr;
                    ys[i] = (float) Math.sin(a) * rr;
                }
                return new float[][]{xs, ys};
            }
            case "Heart" -> {
                n = 40;
                float[] xs = new float[n], ys = new float[n];
                for (int i = 0; i < n; i++) {
                    double t = i * Math.PI * 2 / n;
                    xs[i] = (float) (16 * Math.pow(Math.sin(t), 3)) / 16f;
                    ys[i] = -(float) (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16f;
                }
                return new float[][]{xs, ys};
            }
            default -> { return poly(48, 0, 1f); }
        }
    }

    private static float[][] poly(int n, float startDeg, float scale) {
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(startDeg) + i * Math.PI * 2 / n;
            xs[i] = (float) Math.cos(a) * scale;
            ys[i] = (float) Math.sin(a) * scale;
        }
        return new float[][]{xs, ys};
    }
}
