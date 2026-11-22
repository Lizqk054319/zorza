package pl.zorza.client;

import pl.zorza.client.module.Module;
import pl.zorza.client.modules.*;

import java.util.ArrayList;
import java.util.List;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();
    public static Zoom ZOOM;
    public static HitSounds HIT_SOUNDS;
    public static CustomCrosshair CROSSHAIR;

    private Modules() {}

    public static void init() {
        // HUD
        ALL.add(new FpsHud());
        ALL.add(new CpsHud());
        ALL.add(new CoordinatesHud());
        ALL.add(new ArmorStatusHud());
        ALL.add(new PotionEffectsHud());
        ALL.add(new KeystrokesHud());
        ALL.add(new ItemCounterHud());
        ALL.add(new ClockHud());
        ALL.add(new DirectionHud());
        ALL.add(new PingHud());
        ALL.add(new SpeedHud());
        ALL.add(new ServerAddressHud());
        ALL.add(new MemoryHud());
        // Combat
        ALL.add(new ReachDisplayHud());
        ALL.add(new ComboCounterHud());
        ALL.add(HIT_SOUNDS = new HitSounds());
        ALL.add(new HurtCam());
        // Utility
        ALL.add(ZOOM = new Zoom());
        ALL.add(new ToggleSprint());
        ALL.add(CROSSHAIR = new CustomCrosshair());
        // Client
        ALL.add(new MenuSettings());
    }
}
