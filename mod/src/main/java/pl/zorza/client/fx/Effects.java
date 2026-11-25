package pl.zorza.client.fx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.zorza.client.Modules;
import pl.zorza.client.modules.AmbientParticles;
import pl.zorza.client.modules.JumpCircles;
import pl.zorza.client.modules.JumpParticles;
import pl.zorza.client.modules.Trails;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Efekty Zora Client rysowane w świecie: ślad, kręgi przy skoku, cząsteczki. Wszystko liczone co klatkę (płynnie). */
public final class Effects {
    private Effects() {}

    public enum Shape { STARS, SNOW, HEARTS, SPARKLES, DOTS, BUBBLES }

    public static Shape shape(String name) {
        return switch (name) {
            case "Snow" -> Shape.SNOW;
            case "Hearts" -> Shape.HEARTS;
            case "Sparkles" -> Shape.SPARKLES;
            case "Dots" -> Shape.DOTS;
            case "Bubbles" -> Shape.BUBBLES;
            default -> Shape.STARS;
        };
    }

    public static final List<String> SHAPES = List.of("Stars", "Snow", "Hearts", "Sparkles", "Dots", "Bubbles");

    private static final class P {
        double x, y, z, vx, vy, vz;
        float life, max, size, rot, spin, phase, gravity, sway;
        int color;
        Shape shape;
        boolean ambient;
    }

    private record TrailPoint(Vec3d pos, long time) {}

    private record Circle(Vec3d pos, long time) {}

    private static final List<P> PARTICLES = new ArrayList<>();
    private static final ArrayDeque<TrailPoint> TRAIL = new ArrayDeque<>();
    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static final Random RND = new Random();
    private static long lastFrame = System.nanoTime();
    private static boolean wasOnGround = true;
    private static float ambientAcc = 0;
    private static final int MAX_PARTICLES = 600;

    /** Co tick: wykrywanie skoku. */
    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            PARTICLES.clear();
            TRAIL.clear();
            CIRCLES.clear();
            return;
        }
        boolean on = p.isOnGround();
        if (wasOnGround && !on && p.getVelocity().y > 0.2 && !p.isSwimming() && !p.getAbilities().flying) onJump(p.getPos());
        wasOnGround = on;
    }

    private static void onJump(Vec3d feet) {
        JumpCircles jc = Modules.JUMP_CIRCLES;
        if (jc != null && jc.enabled) CIRCLES.add(new Circle(feet.add(0, 0.03, 0), System.currentTimeMillis()));
        JumpParticles jp = Modules.JUMP_PARTICLES;
        if (jp != null && jp.enabled) {
            int n = (int) jp.amount.value;
            for (int i = 0; i < n && PARTICLES.size() < MAX_PARTICLES; i++) {
                P q = new P();
                double ang = RND.nextDouble() * Math.PI * 2, sp = 0.8 + RND.nextDouble() * 1.4;
                q.x = feet.x + Math.cos(ang) * 0.3;
                q.y = feet.y + 0.1;
                q.z = feet.z + Math.sin(ang) * 0.3;
                q.vx = Math.cos(ang) * sp;
                q.vz = Math.sin(ang) * sp;
                q.vy = 1.2 + RND.nextDouble() * 1.6;
                q.gravity = 4.5f;
                q.max = q.life = 0.7f + RND.nextFloat() * 0.5f;
                q.size = (float) (0.07 + RND.nextFloat() * 0.07) * (float) jp.size.value;
                q.rot = RND.nextFloat() * 360;
                q.spin = (RND.nextFloat() - 0.5f) * 400;
                q.color = jp.rainbow.value ? Shapes.rainbow(RND.nextFloat()) : jp.color.value;
                q.shape = shape(jp.shape.value);
                PARTICLES.add(q);
            }
        }
    }

    private static void spawnAmbient(MinecraftClient mc, Vec3d cam, float dt) {
        AmbientParticles ap = Modules.AMBIENT;
        if (ap == null || !ap.enabled) return;
        ambientAcc += dt * (float) ap.density.value * 25f;
        Shape sh = shape(ap.shape.value);
        while (ambientAcc >= 1 && PARTICLES.size() < MAX_PARTICLES) {
            ambientAcc -= 1;
            P q = new P();
            q.ambient = true;
            q.x = cam.x + (RND.nextDouble() - 0.5) * 36;
            q.z = cam.z + (RND.nextDouble() - 0.5) * 36;
            q.y = cam.y + 6 + RND.nextDouble() * 12;
            boolean falling = sh == Shape.SNOW || sh == Shape.HEARTS || sh == Shape.DOTS;
            q.vy = falling ? -(0.7 + RND.nextDouble() * 0.7) : (sh == Shape.BUBBLES ? 0.3 : -0.15 - RND.nextDouble() * 0.2);
            if (sh == Shape.BUBBLES) q.y = cam.y - 4 + RND.nextDouble() * 6;
            q.vx = (RND.nextDouble() - 0.5) * 0.4;
            q.vz = (RND.nextDouble() - 0.5) * 0.4;
            q.max = q.life = 9 + RND.nextFloat() * 5;
            q.size = (float) (0.08 + RND.nextFloat() * 0.09) * (float) ap.size.value;
            q.rot = RND.nextFloat() * 360;
            q.spin = (RND.nextFloat() - 0.5f) * 60;
            q.phase = RND.nextFloat() * 6.28f;
            q.sway = 0.4f + RND.nextFloat() * 0.6f;
            q.color = ap.rainbow.value ? Shapes.rainbow(RND.nextFloat()) : ap.color.value;
            q.shape = sh;
            PARTICLES.add(q);
        }
        if (ambientAcc > 5) ambientAcc = 0;
    }

    /** Co klatkę (płynnie, niezależnie od TPS). */
    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float delta = ctx.tickCounter().getTickDelta(true);

        updateTrail(mc, delta);
        spawnAmbient(mc, cp, dt);
        boolean any = !PARTICLES.isEmpty() || TRAIL.size() > 1 || !CIRCLES.isEmpty();
        if (!any) return;

        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = imm.getBuffer(RenderLayer.getDebugQuads());
        drawTrail(vc, cp, cam);
        drawCircles(vc, cp);
        drawParticles(vc, cp, cam, dt);
        imm.draw(RenderLayer.getDebugQuads());
    }

    // ---------- ślad ----------
    private static void updateTrail(MinecraftClient mc, float delta) {
        Trails t = Modules.TRAILS;
        long ms = System.currentTimeMillis();
        if (t == null || !t.enabled) {
            TRAIL.clear();
            return;
        }
        long life = (long) (t.length.value * 1000);
        while (!TRAIL.isEmpty() && ms - TRAIL.peekFirst().time() > life) TRAIL.pollFirst();
        Vec3d p = mc.player.getLerpedPos(delta).add(0, t.height.value, 0);
        if (TRAIL.isEmpty() || TRAIL.peekLast().pos().squaredDistanceTo(p) > 0.0009) TRAIL.addLast(new TrailPoint(p, ms));
        while (TRAIL.size() > 400) TRAIL.pollFirst();
    }

    private static void drawTrail(VertexConsumer vc, Vec3d cp, Camera cam) {
        Trails t = Modules.TRAILS;
        if (t == null || !t.enabled || TRAIL.size() < 2) return;
        long ms = System.currentTimeMillis();
        float life = (float) (t.length.value * 1000);
        Matrix4f m = new Matrix4f();
        List<TrailPoint> pts = new ArrayList<>(TRAIL);
        boolean ribbon = "Ribbon".equals(t.style.value);
        float w = (float) t.width.value;
        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            TrailPoint a = pts.get(i), b = pts.get(i + 1);
            float fa = 1 - (ms - a.time()) / life, fb = 1 - (ms - b.time()) / life;
            fa = MathHelper.clamp(fa, 0, 1);
            fb = MathHelper.clamp(fb, 0, 1);
            int ca = t.rainbow.value ? Shapes.rainbow(i / (float) n) : t.color.value;
            int cb = t.rainbow.value ? Shapes.rainbow((i + 1) / (float) n) : t.color.value;
            Vec3d pa = a.pos().subtract(cp), pb = b.pos().subtract(cp);
            if (ribbon) {
                // pionowa wstęga – dół jaśniejszy, góra zanika
                float h = w * 1.6f * fa, hb = w * 1.6f * fb;
                vert(vc, m, pa.x, pa.y, pa.z, Shapes.alpha(ca, fa * 0.75f));
                vert(vc, m, pb.x, pb.y, pb.z, Shapes.alpha(cb, fb * 0.75f));
                vert(vc, m, pb.x, pb.y + hb, pb.z, Shapes.alpha(cb, 0));
                vert(vc, m, pa.x, pa.y + h, pa.z, Shapes.alpha(ca, 0));
            } else {
                // świecąca linia zwrócona do kamery
                Vec3d dir = pb.subtract(pa);
                Vec3d mid = pa.add(pb).multiply(0.5);
                Vec3d side = dir.crossProduct(mid).normalize();
                if (Double.isNaN(side.x)) continue;
                for (int pass = 0; pass < 2; pass++) {
                    float ww = pass == 0 ? w : w * 0.35f, al = pass == 0 ? 0.35f : 0.9f;
                    Vec3d sa = side.multiply(ww * Math.max(0.15, fa) / 2), sb = side.multiply(ww * Math.max(0.15, fb) / 2);
                    vert(vc, m, pa.x + sa.x, pa.y + sa.y, pa.z + sa.z, Shapes.alpha(ca, fa * al));
                    vert(vc, m, pa.x - sa.x, pa.y - sa.y, pa.z - sa.z, Shapes.alpha(ca, fa * al));
                    vert(vc, m, pb.x - sb.x, pb.y - sb.y, pb.z - sb.z, Shapes.alpha(cb, fb * al));
                    vert(vc, m, pb.x + sb.x, pb.y + sb.y, pb.z + sb.z, Shapes.alpha(cb, fb * al));
                }
            }
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, double x, double y, double z, int argb) {
        vc.vertex(m, (float) x, (float) y, (float) z).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24);
    }

    // ---------- kręgi przy skoku ----------
    private static void drawCircles(VertexConsumer vc, Vec3d cp) {
        JumpCircles jc = Modules.JUMP_CIRCLES;
        long ms = System.currentTimeMillis();
        if (jc == null) return;
        float life = (float) (jc.duration.value * 1000);
        CIRCLES.removeIf(c -> ms - c.time() > life);
        for (Circle c : CIRCLES) {
            float f = (ms - c.time()) / life;                      // 0..1
            float ease = 1 - (1 - f) * (1 - f) * (1 - f);
            float r = (float) (0.15 + ease * jc.radius.value);
            float alpha = 1 - f;
            int col = jc.rainbow.value ? Shapes.rainbow(f * 0.5f) : jc.color.value;
            MatrixStack ms2 = new MatrixStack();
            ms2.translate(c.pos().x - cp.x, c.pos().y - cp.y, c.pos().z - cp.z);
            ms2.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            ms2.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * (float) jc.spin.value));
            Matrix4f m = ms2.peek().getPositionMatrix();
            float[][] shape = JumpCircles.outline(jc.shape.value);
            float th = (float) (0.07 * (1 - f * 0.5));
            Shapes.ring(vc, m, shape[0], shape[1], r, th * 3.2f, Shapes.alpha(col, alpha * 0.22f));      // poświata
            Shapes.ring(vc, m, shape[0], shape[1], r, th, Shapes.alpha(col, alpha * 0.95f));
            if ("Double ring".equals(jc.shape.value)) Shapes.ring(vc, m, shape[0], shape[1], r * 0.6f, th * 0.8f, Shapes.alpha(col, alpha * 0.7f));
        }
    }

    // ---------- cząsteczki ----------
    private static void drawParticles(VertexConsumer vc, Vec3d cp, Camera cam, float dt) {
        Iterator<P> it = PARTICLES.iterator();
        boolean ambientOn = Modules.AMBIENT != null && Modules.AMBIENT.enabled;
        while (it.hasNext()) {
            P q = it.next();
            q.life -= dt;
            if (q.life <= 0 || (q.ambient && (!ambientOn || q.y < cp.y - 14 || q.y > cp.y + 24))) {
                it.remove();
                continue;
            }
            q.vy -= q.gravity * dt;
            if (!q.ambient) {
                q.vx *= Math.pow(0.15, dt);
                q.vz *= Math.pow(0.15, dt);
            }
            q.phase += dt;
            double sway = q.ambient ? Math.sin(q.phase * 1.3) * q.sway * dt : 0;
            q.x += q.vx * dt + sway;
            q.y += q.vy * dt;
            q.z += q.vz * dt + (q.ambient ? Math.cos(q.phase * 1.1) * q.sway * dt * 0.7 : 0);
            q.rot += q.spin * dt;
            float lf = q.life / q.max;
            float a = q.ambient ? Math.min(1, Math.min((q.max - q.life) * 1.5f, q.life * 0.8f)) : Math.min(1, lf * 2.2f);
            if (q.shape == Shape.STARS || q.shape == Shape.SPARKLES) a *= 0.75f + 0.25f * (float) Math.sin(q.phase * 6 + q.rot);
            MatrixStack s = new MatrixStack();
            s.translate(q.x - cp.x, q.y - cp.y, q.z - cp.z);
            s.multiply(cam.getRotation());
            s.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(q.rot));
            float size = q.size * (q.ambient ? 1 : 0.6f + 0.4f * lf);
            s.scale(size, size, size);
            Matrix4f m = s.peek().getPositionMatrix();
            int col = Shapes.alpha(q.color, a);
            switch (q.shape) {
                case STARS -> {
                    Shapes.circle(vc, m, 12, Shapes.alpha(col, 0.25f), Shapes.alpha(col, 0));
                    Shapes.star(vc, m, 5, 0.45f, col);
                }
                case SNOW -> Shapes.snowflake(vc, m, col);
                case HEARTS -> Shapes.heart(vc, m, col);
                case SPARKLES -> {
                    Shapes.circle(vc, m, 12, Shapes.alpha(col, 0.3f), Shapes.alpha(col, 0));
                    Shapes.star(vc, m, 4, 0.22f, col);
                }
                case DOTS -> Shapes.circle(vc, m, 14, col, Shapes.alpha(col, 0.1f));
                case BUBBLES -> {
                    float[][] c = JumpCircles.outline("Circle");
                    Shapes.ring(vc, m, c[0], c[1], 0.85f, 0.18f, col);
                    Shapes.circle(vc, m, 14, Shapes.alpha(col, 0.12f), Shapes.alpha(col, 0.05f));
                }
            }
        }
    }
}
