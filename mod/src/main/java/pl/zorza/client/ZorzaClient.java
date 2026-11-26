package pl.zorza.client;

import pl.zorza.client.compat.Ids;

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
import pl.zorza.client.module.HudModule;
import pl.zorza.client.module.Module;

import java.util.ArrayDeque;
import java.util.Deque;

public class ZorzaClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Zorza");
    public static KeyBinding MENU_KEY;
    public static KeyBinding ZOOM_KEY;
    public static KeyBinding FREELOOK_KEY;
    public static KeyBinding HEALTH_UP_KEY;
    public static KeyBinding HEALTH_DOWN_KEY;

    private static final Deque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    public static double lastReach = -1;
    public static long lastReachTime = 0;
    public static int combo = 0;
    public static long lastHitTime = 0;
    private static int lastHurtTime = 0;
    private static double zoom = 1.0;
    private static long lastFrame = System.nanoTime();

    //#if MC >= 1.21.9
    //$$ private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(pl.zorza.client.compat.Ids.of("zorza", "main"));
    //#else
    private static final String CATEGORY = "category.zorza";
    //#endif

    @Override
    public void onInitializeClient() {
        Modules.init();
        Config.load();
        CustomSounds.init();
        AutoJoin.register();
        //#if MC < 1.21.4
        //$$ SmokeTest.init();
        //#endif
        // gładka czcionka Inter w całej grze (można wyłączyć w Resource Packs)
        net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("zorza").ifPresent(mod ->
                net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                        Ids.of("zorza", "zora_font"), mod,
                        net.minecraft.text.Text.literal("Zora Font"),
                        net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED));
        net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("zorza").ifPresent(mod ->
                net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                        Ids.of("zorza", "zora_dark_gui"), mod,
                        net.minecraft.text.Text.literal("Zora Dark GUI"),
                        net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED));
        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        ZOOM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
        FREELOOK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
        HEALTH_UP_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.health_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY));
        HEALTH_DOWN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zorza.health_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(ZorzaClient::tick);
        //#if MC >= 1.21.10
        //$$ net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AFTER_ENTITIES.register(c -> {
        //$$     MinecraftClient mc = MinecraftClient.getInstance();
        //$$     var w = new pl.zorza.client.compat.WorldCtx(mc.gameRenderer.getCamera(), pl.zorza.client.compat.Gfx.frameDelta(mc));
        //$$     Modules.HEALTH.render(w);
        //$$     pl.zorza.client.fx.Effects.render(w);
        //$$ });
        //#elseif MC >= 1.21.9
        //$$ // 1.21.9: Fabric API nie ma tu zdarzeń rysowania świata – efekty w świecie są od 1.21.10
        //#else
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(c -> {
            var w = new pl.zorza.client.compat.WorldCtx(c.camera(), pl.zorza.client.compat.Gfx.worldDelta(c));
            Modules.HEALTH.render(w);
            pl.zorza.client.fx.Effects.render(w);
        });
        //#endif
                HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.player == null) return;
            if (mc.currentScreen instanceof HudEditorScreen) return;
            //#if MC >= 1.20.2
            if (mc.getDebugHud().shouldShowDebugHud()) return;
            //#else
            //$$ if (mc.options.debugEnabled) return;
            //#endif
            HudModule.FRAME++;
            for (Module m : Modules.ALL) {
                if (m.enabled && m instanceof HudModule h) h.render(ctx, mc, false);
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            //#if MC >= 1.21.9
            //$$ if (world.isClient()) {
            //#else
            if (world.isClient) {
            //#endif
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
                Modules.KILL_SOUNDS.onHit(entity);
            }
            return ActionResult.PASS;
        });
        LOG.info("Zora Client gotowy – prawy Shift otwiera menu.");
    }

    private static void tick(MinecraftClient mc) {
        CustomSounds.cleanup();
        Modules.FREELOOK.update(mc);
        Modules.HEALTH.tickKeys(mc);
        pl.zorza.client.fx.Effects.tick(mc);
        while (MENU_KEY.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new HudEditorScreen(true));
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
