package pl.zorza.client.compat;

import net.minecraft.client.gui.DrawContext;

/**
 * Przesuwanie/skalowanie rysowania w GUI. Od 1.21.6 GUI ma macierze 2D (bez osi Z),
 * wcześniej 3D – mod woła tylko te metody.
 */
public final class M {
    private M() {
    }

    public static void push(DrawContext c) {
        //#if MC >= 1.21.6
        //$$ c.getMatrices().pushMatrix();
        //#else
        c.getMatrices().push();
        //#endif
    }

    public static void pop(DrawContext c) {
        //#if MC >= 1.21.6
        //$$ c.getMatrices().popMatrix();
        //#else
        c.getMatrices().pop();
        //#endif
    }

    public static void translate(DrawContext c, float x, float y) {
        //#if MC >= 1.21.6
        //$$ c.getMatrices().translate(x, y);
        //#else
        c.getMatrices().translate(x, y, 0f);
        //#endif
    }

    /** Z ma znaczenie tylko przed 1.21.6 (wyższe Z = rysowane na wierzchu). */
    public static void translate(DrawContext c, float x, float y, float z) {
        //#if MC >= 1.21.6
        //$$ c.getMatrices().translate(x, y);
        //#else
        c.getMatrices().translate(x, y, z);
        //#endif
    }

    public static void scale(DrawContext c, float sx, float sy) {
        //#if MC >= 1.21.6
        //$$ c.getMatrices().scale(sx, sy);
        //#else
        c.getMatrices().scale(sx, sy, 1f);
        //#endif
    }

    public static void identity(DrawContext c) {
        //#if MC >= 1.21.6
        //$$ c.getMatrices().identity();
        //#else
        c.getMatrices().loadIdentity();
        //#endif
    }
}
