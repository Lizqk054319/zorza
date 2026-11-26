package pl.zorza.client.compat;

import net.minecraft.util.Identifier;

/** Identifier.of pojawił się w 1.21 – wcześniej był konstruktor. */
public final class Ids {
    private Ids() {
    }

    public static Identifier of(String ns, String path) {
        //#if MC >= 1.21
        return Identifier.of(ns, path);
        //#else
        //$$ return new Identifier(ns, path);
        //#endif
    }

    public static Identifier mc(String path) {
        return of("minecraft", path);
    }
}
