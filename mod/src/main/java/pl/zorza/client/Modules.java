package pl.zorza.client;

import pl.zorza.client.module.Module;
import pl.zorza.client.modules.*;

import java.util.ArrayList;
import java.util.List;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();
    public static Zoom ZOOM;
    public static HitSounds HIT_SOUNDS;
    public static KillSounds KILL_SOUNDS;
    public static Gamma GAMMA;
    public static Freelook FREELOOK;
    public static LowFire LOW_FIRE;
    public static TimeChanger TIME_CHANGER;
    public static ClearWeather CLEAR_WEATHER;
    public static ViewModel VIEW_MODEL;
    public static HealthIndicators HEALTH;
    public static HudScale HUD_SCALE;
    public static Trails TRAILS;
    public static JumpCircles JUMP_CIRCLES;
    public static JumpParticles JUMP_PARTICLES;
    public static AmbientParticles AMBIENT;
    public static MotionBlur MOTION_BLUR;
    public static CustomHitboxes CUSTOM_HITBOXES;
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
        ALL.add(KILL_SOUNDS = new KillSounds());
        ALL.add(GAMMA = new Gamma());
        ALL.add(FREELOOK = new Freelook());
        ALL.add(LOW_FIRE = new LowFire());
        ALL.add(TIME_CHANGER = new TimeChanger());
        ALL.add(CLEAR_WEATHER = new ClearWeather());
        ALL.add(VIEW_MODEL = new ViewModel());
        ALL.add(HEALTH = new HealthIndicators());
        ALL.add(new CooldownsHud());
        ALL.add(HUD_SCALE = new HudScale());
        ALL.add(TRAILS = new Trails());
        ALL.add(JUMP_CIRCLES = new JumpCircles());
        ALL.add(JUMP_PARTICLES = new JumpParticles());
        ALL.add(AMBIENT = new AmbientParticles());
        ALL.add(MOTION_BLUR = new MotionBlur());
        ALL.add(CUSTOM_HITBOXES = new CustomHitboxes());
        ALL.add(new DayCounterHud());
        ALL.add(new HurtCam());
        // Utility
        ALL.add(ZOOM = new Zoom());
        ALL.add(new ToggleSprint());
        ALL.add(CROSSHAIR = new CustomCrosshair());
        // Client
        ALL.add(new MenuSettings());
    }
}
