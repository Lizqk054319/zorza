package pl.zorza.client.modules;

import pl.zorza.client.compat.Ids;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.zorza.client.Config;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.Locale;

/** Serca nad głowami graczy – prawdziwe serca z Minecrafta (także z paczek zasobów), po 10 w rzędzie, rzędy w górę. */
public class HealthIndicators extends Module {
    private static final Style VANILLA = pl.zorza.client.compat.Gfx.font(Style.EMPTY, Ids.of("zorza", "vanilla"));
    private static final Identifier CONTAINER = Ids.mc("hud/heart/container"),
            FULL = Ids.mc("hud/heart/full"), HALF = Ids.mc("hud/heart/half"),
            ABS_FULL = Ids.mc("hud/heart/absorbing_full"), ABS_HALF = Ids.mc("hud/heart/absorbing_half");

    public final Setting.Num offset = add(new Setting.Num("offset", "Height (↑ ↓ keys)", 0, 0, 30, 0.5));
    public final Setting.Bool mobs = add(new Setting.Bool("mobs", "Also show for mobs", false));
    public final Setting.Num distance = add(new Setting.Num("distance", "Max distance", 32, 8, 64, 1));
    private long changedAt = 0;

    public HealthIndicators() {
        super("health_indicators", "Player Health", "Minecraft hearts above other players' heads. Arrow keys ↑ ↓ change the height.", Category.COMBAT, false);
    }

    public String icon() { return "m_heart"; }

    /** Strzałki ↑ ↓: podnoszenie serc o 0.5 (0 – 30). */
    public void tickKeys(MinecraftClient mc) {
        boolean changed = false;
        while (ZorzaClient.HEALTH_UP_KEY.wasPressed()) {
            if (enabled && mc.currentScreen == null) { offset.set(offset.value + 0.5); changed = true; }
        }
        while (ZorzaClient.HEALTH_DOWN_KEY.wasPressed()) {
            if (enabled && mc.currentScreen == null) { offset.set(offset.value - 0.5); changed = true; }
        }
        if (changed) {
            changedAt = System.currentTimeMillis();
            Config.save();
        }
    }

    private static float[] uv(Sprite s) {
        return new float[]{s.getMinU(), s.getMinV(), s.getMaxU(), s.getMaxV()};
    }

    private static float[] icon(int u) {
        return new float[]{u / 256f, 0f, (u + 9) / 256f, 9 / 256f};
    }

    /** Bufor + współrzędne tekstury jednego rodzaju serca. */
    private record H(VertexConsumer fixed, VertexConsumerProvider vcp, net.minecraft.client.render.RenderLayer layer, float[] uv) {
        H(VertexConsumer vc, float[] uv) {
            this(vc, null, null, uv);
        }

        /** Osobne tekstury (1.21.9+) – bufor bierzemy przy każdym sercu, bo zmiana warstwy zamyka poprzedni. */
        VertexConsumer vc() {
            return fixed != null ? fixed : vcp.getBuffer(layer);
        }
    }

    //#if MC >= 1.21.9
    //$$ private static H tex(VertexConsumerProvider vcp, String sprite) {
    //$$     return new H(null, vcp, pl.zorza.client.compat.Gfx.textLayer(Ids.mc("textures/gui/sprites/" + sprite + ".png")), new float[]{0f, 0f, 1f, 1f});
    //$$ }
    //#endif

    private static void quad(H h, Matrix4f m, float x, float y, float size, int light) {
        VertexConsumer vc = h.vc();
        float[] s = h.uv();
        //#if MC >= 1.21
        vc.vertex(m, x, y, 0).color(255, 255, 255, 255).texture(s[0], s[1]).light(light);
        vc.vertex(m, x, y + size, 0).color(255, 255, 255, 255).texture(s[0], s[3]).light(light);
        vc.vertex(m, x + size, y + size, 0).color(255, 255, 255, 255).texture(s[2], s[3]).light(light);
        vc.vertex(m, x + size, y, 0).color(255, 255, 255, 255).texture(s[2], s[1]).light(light);
        //#else
        //$$ vc.vertex(m, x, y, 0).color(255, 255, 255, 255).texture(s[0], s[1]).light(light).next();
        //$$ vc.vertex(m, x, y + size, 0).color(255, 255, 255, 255).texture(s[0], s[3]).light(light).next();
        //$$ vc.vertex(m, x + size, y + size, 0).color(255, 255, 255, 255).texture(s[2], s[3]).light(light).next();
        //$$ vc.vertex(m, x + size, y, 0).color(255, 255, 255, 255).texture(s[2], s[1]).light(light).next();
        //#endif
    }

    /** Rysuje serca (od dołu w górę) – zwraca wysokość w pikselach. */
    private int drawHearts(VertexConsumerProvider vcp, Matrix4f m, LivingEntity e, int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        //#if MC >= 1.21.9
        //$$ // 1.21.9+: każde serce to osobny plik tekstury (z paczek zasobów też)
        //$$ H cont = tex(vcp, "hud/heart/container"), full = tex(vcp, "hud/heart/full"), half = tex(vcp, "hud/heart/half"),
        //$$         absF = tex(vcp, "hud/heart/absorbing_full"), absH = tex(vcp, "hud/heart/absorbing_half");
        //#elseif MC >= 1.20.2
        var atlas = mc.getGuiAtlasManager();
        VertexConsumer vc = vcp.getBuffer(pl.zorza.client.compat.Gfx.textLayer(atlas.getSprite(FULL).getAtlasId()));
        H cont = new H(vc, uv(atlas.getSprite(CONTAINER))), full = new H(vc, uv(atlas.getSprite(FULL))), half = new H(vc, uv(atlas.getSprite(HALF))),
                absF = new H(vc, uv(atlas.getSprite(ABS_FULL))), absH = new H(vc, uv(atlas.getSprite(ABS_HALF)));
        //#else
        //$$ // 1.20.1: serca są w textures/gui/icons.png (256x256)
        //$$ VertexConsumer vc = vcp.getBuffer(pl.zorza.client.compat.Gfx.textLayer(Ids.mc("textures/gui/icons.png")));
        //$$ H cont = new H(vc, icon(16)), full = new H(vc, icon(52)), half = new H(vc, icon(61)), absF = new H(vc, icon(160)), absH = new H(vc, icon(169));
        //#endif
        int hp = (int) Math.ceil(Math.max(0, e.getHealth())), abs = (int) Math.ceil(e.getAbsorptionAmount());
        int maxHearts = (int) Math.ceil(Math.max(1, e.getMaxHealth()) / 2f);
        int absHearts = (int) Math.ceil(abs / 2f);
        int total = Math.min(100, maxHearts + absHearts);
        int rows = (total + 9) / 10;
        for (int i = 0; i < total; i++) {
            int row = i / 10, col = i % 10;
            int inRow = Math.min(10, total - row * 10);
            float x = -(inRow * 8 + 1) / 2f + col * 8;
            float y = -row * 10f;                                    // kolejne rzędy w górę
            if (i < maxHearts) {
                quad(cont, m, x, y, 9, light);
                int v = hp - i * 2;
                if (v >= 2) quad(full, m, x, y, 9, light);
                else if (v == 1) quad(half, m, x, y, 9, light);
            } else {
                int v = abs - (i - maxHearts) * 2;
                quad(cont, m, x, y, 9, light);
                quad(v >= 2 ? absF : absH, m, x, y, 9, light);
            }
        }
        return rows * 10;
    }

    public void render(pl.zorza.client.compat.WorldCtx ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        Camera cam = ctx.camera();
        Vec3d cp = pl.zorza.client.compat.Gfx.camPos(cam);
        float delta = ctx.delta();
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate vc = mc.getBufferBuilders().getEntityVertexConsumers();
        double maxD = distance.value * distance.value;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        boolean showChange = System.currentTimeMillis() - changedAt < 1500;
        boolean drew = false;
        for (Entity en : mc.world.getEntities()) {
            if (!(en instanceof LivingEntity le) || en == mc.player || en == cam.getFocusedEntity()) continue;
            if (!(en instanceof PlayerEntity) && !mobs.value) continue;
            if (en.isInvisibleTo(mc.player) || !le.isAlive()) continue;
            if (en.squaredDistanceTo(cp) > maxD) continue;
            Vec3d p = en.getLerpedPos(delta);
            boolean tag = en instanceof PlayerEntity || en.hasCustomName();
            MatrixStack ms = new MatrixStack();
            ms.translate(p.x - cp.x, p.y - cp.y + en.getHeight() + (tag ? 0.8 : 0.5) + offset.value * 0.1, p.z - cp.z);
            ms.multiply(cam.getRotation());
            ms.scale(0.025f, -0.025f, 0.025f);
            Matrix4f m = ms.peek().getPositionMatrix();
            float top = drawHearts(vc, m, le, light) - 10;         // same serca – bez liczby nad nimi
            if (showChange) {
                String s = (mc.getLanguageManager().getLanguage().startsWith("pl") ? "Podniesione o " : "Raised by ")
                        + String.format(Locale.ROOT, "%.1f", offset.value);
                Text t = Text.literal(s).setStyle(VANILLA.withColor(0xFFE066));
                tr.draw(t, -tr.getWidth(t) / 2f, -top - 12, 0xFFFFFFFF, false, m, vc, TextRenderer.TextLayerType.NORMAL, 0x66000000, light);
            }
            drew = true;
        }
        if (drew) vc.draw();
    }
}
