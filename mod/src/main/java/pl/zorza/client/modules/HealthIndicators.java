package pl.zorza.client.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.List;
import java.util.Locale;

/** Serduszka (albo liczba HP) nad głowami graczy – jak Player Health Indicators. */
public class HealthIndicators extends Module {
    private static final Style VANILLA = Style.EMPTY.withFont(Identifier.of("zorza", "vanilla"));
    private static final Style HEARTS = Style.EMPTY.withFont(Identifier.of("zorza", "hearts")).withColor(0xFFFFFF);
    public final Setting.Choice mode = add(new Setting.Choice("mode", "Display", "Hearts", () -> List.of("Hearts", "Number", "Both")));
    public final Setting.Bool mobs = add(new Setting.Bool("mobs", "Also show for mobs", false));
    public final Setting.Num distance = add(new Setting.Num("distance", "Max distance", 32, 8, 64, 1));
    public final Setting.Bool background = add(new Setting.Bool("background", "Background", true));

    public HealthIndicators() {
        super("health_indicators", "Player Health", "Shows hearts above other players' heads.", Category.COMBAT, false);
    }

    public String icon() { return "m_heart"; }

    private Text label(LivingEntity e) {
        float hp = Math.max(0, e.getHealth()), abs = e.getAbsorptionAmount(), max = Math.max(1, e.getMaxHealth());
        MutableText t = Text.empty().setStyle(VANILLA);
        if (!"Number".equals(mode.value)) {
            // pikselowe serca jak w Minecrafcie: pełne, połówki i puste (+ złote za absorpcję)
            int hearts = Math.min(10, (int) Math.ceil(max / 2f));
            float per = max / (hearts * 2f);                         // przy >20 HP jedno serce = więcej życia
            int halves = Math.round(hp / per);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hearts; i++) {
                sb.append(halves >= 2 ? '\ue000' : halves == 1 ? '\ue001' : '\ue002');
                halves -= 2;
            }
            int absHalves = Math.min(20, Math.round(abs));
            while (absHalves > 0) {
                sb.append(absHalves >= 2 ? '\ue003' : '\ue004');
                absHalves -= 2;
            }
            t.append(Text.literal(sb.toString()).setStyle(HEARTS));
        }
        if (!"Hearts".equals(mode.value)) {
            String n = String.format(Locale.ROOT, hp % 1 == 0 ? "%.0f" : "%.1f", hp);
            if ("Number".equals(mode.value)) t.append(Text.literal("\ue000 ").setStyle(HEARTS));
            else t.append(Text.literal(" ").setStyle(VANILLA));
            t.append(Text.literal(n).setStyle(VANILLA.withColor(0xFFFFFF)));
            if (abs > 0) t.append(Text.literal(String.format(Locale.ROOT, " +%.0f", abs)).setStyle(VANILLA.withColor(0xFFD43B)));
        }
        return t;
    }

    public void render(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float delta = ctx.tickCounter().getTickDelta(true);
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate vc = mc.getBufferBuilders().getEntityVertexConsumers();
        double maxD = distance.value * distance.value;
        boolean drew = false;
        for (Entity en : mc.world.getEntities()) {
            if (!(en instanceof LivingEntity le) || en == mc.player || en == cam.getFocusedEntity()) continue;
            if (!(en instanceof PlayerEntity) && !mobs.value) continue;
            if (en.isInvisibleTo(mc.player) || !le.isAlive()) continue;
            if (en.squaredDistanceTo(cp) > maxD) continue;
            Vec3d p = en.getLerpedPos(delta);
            boolean tag = en instanceof PlayerEntity || en.hasCustomName();
            MatrixStack m = new MatrixStack();
            m.translate(p.x - cp.x, p.y - cp.y + en.getHeight() + (tag ? 0.78 : 0.5), p.z - cp.z);
            m.multiply(cam.getRotation());
            m.scale(0.025f, -0.025f, 0.025f);
            Text t = label(le);
            float x = -tr.getWidth(t) / 2f;
            int bg = background.value ? 0x55000000 : 0;
            tr.draw(t, x, 0, 0xFFFFFFFF, false, m.peek().getPositionMatrix(), vc, TextRenderer.TextLayerType.NORMAL, bg,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
            drew = true;
        }
        if (drew) vc.draw();
    }
}
