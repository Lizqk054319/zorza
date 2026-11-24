package pl.zorza.client;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.modules.MenuSettings;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** Animacja przewijania list – co klatkę przesuwa listę w stronę celu. */
public final class SmoothScroll {
    public interface Access {
        double zorza$get();

        void zorza$set(double y);
    }

    private static final class State {
        double target, last;
        long time;
    }

    private static final Map<Access, State> ACTIVE = new WeakHashMap<>();

    private SmoothScroll() {}

    public static boolean enabled() {
        return MenuSettings.INSTANCE == null || MenuSettings.INSTANCE.animations.value;
    }

    public static void start(Access w, double from, double to) {
        to = from + (to - from) * 2.0;                 // szybciej niż zwykle w Minecrafcie
        State s = ACTIVE.get(w);
        if (s == null || Math.abs(w.zorza$get() - s.last) > 0.5) {   // nowa animacja albo ktoś przeciągnął pasek
            s = new State();
            s.target = to;
            ACTIVE.put(w, s);
        } else {
            s.target += to - from;
        }
        s.last = from;
        s.time = System.nanoTime();
    }

    /** Wywoływane co klatkę. */
    public static void frame() {
        if (ACTIVE.isEmpty()) return;
        long now = System.nanoTime();
        for (Iterator<Map.Entry<Access, State>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Access, State> e = it.next();
            Access w = e.getKey();
            State s = e.getValue();
            double cur = w.zorza$get();
            if (Math.abs(cur - s.last) > 0.5) { it.remove(); continue; }     // przewinięte inaczej (np. pasek)
            double dt = Math.min(0.05, (now - s.time) / 1e9);
            s.time = now;
            double k = 1 - Math.exp(-dt * 20);                               // wygładzenie niezależne od FPS
            double next = cur + (s.target - cur) * k;
            if (Math.abs(s.target - next) < 0.4) next = s.target;
            w.zorza$set(next);
            s.last = w.zorza$get();
            if (next == s.target || s.last != next) it.remove();            // koniec albo dobiliśmy do krawędzi
        }
    }
}
