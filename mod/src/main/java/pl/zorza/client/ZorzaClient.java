package pl.zorza.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.zorza.client.gui.HudEditorScreen;
import pl.zorza.client.gui.QuickMenuScreen;
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Module;

import java.util.ArrayDeque;
import java.util.Deque;

public class ZorzaClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Zorza");
    public static KeyBinding MENU_KEY;
    public static KeyBinding ZOOM_KEY;

    private static final Deque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    public static double lastReach = -1;
    public static long lastReachTime = 0;
    public static int combo = 0;
    public static long lastHitTime = 0;
    private static int lastHurtTime = 0;
    private static double zoom = 1.0;
    private static long lastFrame = System.nanoTime();

    @Override
    public void onInitializeClient() {
        Modules.init();
        Config.load();
        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.zorza"));
        ZOOM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.zorza"));

        ClientTickEvents.END_CLIENT_TICK.register(ZorzaClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.player == null) return;
            if (mc.currentScreen instanceof HudEditorScreen) return;
            if (mc.getDebugHud().shouldShowDebugHud()) return;
            for (Module m : Modules.ALL) {
                if (m.enabled && m instanceof HudModule h) h.render(ctx, mc, false);
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) {
                Vec3d eye = player.getEyePos();
                Box b = entity.getBoundingBox();
                double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
                double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
                double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
                lastReach = eye.distanceTo(new Vec3d(x, y, z));
                long now = System.currentTimeMillis();
                lastReachTime = now;
                combo = now - lastHitTime > 3000 ? 1 : combo + 1;
                lastHitTime = now;
                Modules.HIT_SOUNDS.onHit();
            }
            return ActionResult.PASS;
        });
        LOG.info("Zorza Client gotowy – prawy Shift otwiera menu.");
    }

    private static void tick(MinecraftClient mc) {
        while (MENU_KEY.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new QuickMenuScreen());
        }
        if (mc.player != null) {
            int ht = mc.player.hurtTime;
            if (ht > 0 && lastHurtTime == 0) combo = 0;
            lastHurtTime = ht;
            if (System.currentTimeMillis() - lastHitTime > 3000) combo = 0;
        }
        for (Module m : Modules.ALL) {
            if (m.enabled) m.onTick(mc);
        }
    }

    public static void onClick(int button) {
        long now = System.currentTimeMillis();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) LEFT.addLast(now);
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) RIGHT.addLast(now);
    }

    private static int cps(Deque<Long> d) {
        long now = System.currentTimeMillis();
        while (!d.isEmpty() && now - d.peekFirst() > 1000) d.pollFirst();
        return d.size();
    }

    public static int leftCps() { return cps(LEFT); }

    public static int rightCps() { return cps(RIGHT); }

    /** Płynny zoom – wywoływane co klatkę z GameRenderer.getFov. */
    public static float zoomDivisor() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean active = Modules.ZOOM.enabled && ZOOM_KEY != null && ZOOM_KEY.isPressed() && mc.currentScreen == null;
        double target = active ? Modules.ZOOM.factor.value : 1.0;
        if (!Modules.ZOOM.smooth.value) zoom = target;
        else zoom += (target - zoom) * Math.min(1.0, dt * 14.0);
        return (float) zoom;
    }
}
