package pl.zorza.client.fx;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
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

    public static void afterWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        if (Modules.MOTION_BLUR == null || !Modules.MOTION_BLUR.enabled || mc.world == null) {
            valid = false;
            return;
        }
        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        if (prev == null) prev = new SimpleFramebuffer(w, h, false);
        if (prev.textureWidth != w || prev.textureHeight != h) {
            prev.resize(w, h);
            valid = false;
        }
        float strength = (float) Modules.MOTION_BLUR.strength.value / 100f;
        // 0-100% -> do 0.82 przy 60 FPS; przy innym FPS taki sam efekt w czasie
        float a = (float) Math.pow(0.82f * strength, dt * 60f);
        if (strength <= 0.001f) a = 0;
        if (valid && a > 0.01f) {
            main.beginWrite(false);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, prev.getColorAttachment());
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, w, 0, h, -1000, 1000), ProjectionType.ORTHOGRAPHIC);
            Matrix4fStack mv = RenderSystem.getModelViewStack();
            mv.pushMatrix();
            mv.identity();
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            b.vertex(0, 0, 0).texture(0, 0).color(1f, 1f, 1f, a);
            b.vertex(w, 0, 0).texture(1, 0).color(1f, 1f, 1f, a);
            b.vertex(w, h, 0).texture(1, 1).color(1f, 1f, 1f, a);
            b.vertex(0, h, 0).texture(0, 1).color(1f, 1f, 1f, a);
            BufferRenderer.drawWithGlobalProgram(b.end());
            mv.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
        // zapamiętaj tę klatkę
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prev.fbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.beginWrite(false);
        valid = true;
    }
}
