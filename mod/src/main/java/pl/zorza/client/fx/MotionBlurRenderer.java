package pl.zorza.client.fx;

//#if MC >= 1.21.2
import com.mojang.blaze3d.systems.ProjectionType;
import net.minecraft.client.gl.ShaderProgramKeys;
//#else
//$$ import com.mojang.blaze3d.systems.VertexSorter;
//$$ import net.minecraft.client.render.GameRenderer;
//#endif
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL14;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import pl.zorza.client.Modules;

/**
 * Motion blur: po narysowaniu świata nakładamy poprzednią klatkę z przezroczystością,
 * a potem zapamiętujemy wynik jako "poprzednią klatkę". HUD rysuje się później – zostaje ostry.
 */
public final class MotionBlurRenderer {
    private static Framebuffer prev;
    private static boolean valid = false;
    private static long last = System.nanoTime();

    private MotionBlurRenderer() {}

    private static void drawTexture(Framebuffer target, Framebuffer source, int w, int h, float a, boolean blend) {
        target.beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        if (blend) {
            // stała przezroczystość – kanał alfa klatki Minecrafta bywa 0, więc nie można na nim polegać
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.CONSTANT_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            GL14.glBlendColor(0f, 0f, 0f, a);
        } else RenderSystem.disableBlend();
        //#if MC >= 1.21.2
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        //#else
        //$$ RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        //#endif
        RenderSystem.setShaderTexture(0, source.getColorAttachment());
        RenderSystem.backupProjectionMatrix();
        //#if MC >= 1.21.2
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, w, 0, h, -1000, 1000), ProjectionType.ORTHOGRAPHIC);
        //#else
        //$$ RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, w, 0, h, -1000, 1000), VertexSorter.BY_Z);
        //#endif
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(0, 0, 0).texture(0, 0).color(1f, 1f, 1f, 1f);
        b.vertex(w, 0, 0).texture(1, 0).color(1f, 1f, 1f, 1f);
        b.vertex(w, h, 0).texture(1, 1).color(1f, 1f, 1f, 1f);
        b.vertex(0, h, 0).texture(0, 1).color(1f, 1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(b.end());
        mv.popMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        if (blend) {
            GL14.glBlendColor(0f, 0f, 0f, 0f);
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
    }

    private static float histAge = 0;
    private static float lastYaw, lastPitch;

    /*
     * Dlaczego były smugi: przy wysokim FPS (np. 1600) każda klatka mieszała ~96% starego obrazu.
     * 8-bitowe kolory zaokrąglały wtedy zmianę do zera – stary obraz "przyklejał się" na stałe.
     * Teraz: historia odświeża się najwyżej ~60 razy na sekundę, mieszanie ma rozsądny limit,
     * a gdy kamera stoi w miejscu, rozmycie szybko gaśnie (żadnych duchów za mobami).
     */
    public static void afterWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        if (Modules.MOTION_BLUR == null || !Modules.MOTION_BLUR.enabled || mc.world == null || mc.player == null) {
            valid = false;
            return;
        }
        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        //#if MC >= 1.21.2
        if (prev == null) prev = new SimpleFramebuffer(w, h, false);
        //#else
        //$$ if (prev == null) prev = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
        //#endif
        if (prev.textureWidth != w || prev.textureHeight != h) {
            //#if MC >= 1.21.2
            prev.resize(w, h);
            //#else
            //$$ prev.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
            //#endif
            valid = false;
        }
        float strength = (float) Modules.MOTION_BLUR.strength.value / 100f;
        // ruch kamery (stopnie na sekundę) – rozmywamy głównie przy obracaniu
        float yaw = mc.gameRenderer.getCamera().getYaw(), pitch = mc.gameRenderer.getCamera().getPitch();
        float dYaw = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(yaw - lastYaw)), dPitch = Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;
        float speed = dt > 0 ? (dYaw + dPitch) / dt : 0;
        if (speed > 4000) valid = false;                       // teleport / nagły skok kamery – bez smugi
        float motion = Math.min(1f, speed / 180f);             // pełne rozmycie od ~180°/s
        float a = 0.62f * strength * (0.25f + 0.75f * motion);
        if (valid && a > 0.02f) drawTexture(main, prev, w, h, a, true);
        // historię odświeżamy co ~1/60 s (przy wysokim FPS nie kumulujemy błędów zaokrągleń)
        histAge += dt;
        if (!valid || histAge >= 1f / 60f) {
            drawTexture(prev, main, w, h, 1f, false);
            histAge = 0;
        }
        main.beginWrite(false);
        valid = true;
    }
}
