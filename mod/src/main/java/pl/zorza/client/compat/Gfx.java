package pl.zorza.client.compat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

/**
 * Rysowanie tekstur w GUI – w każdej wersji Minecrafta wygląda to trochę inaczej,
 * więc cały mod woła tylko te metody, a różnice są tutaj.
 */
public final class Gfx {
    private Gfx() {
    }

    /** Cała tekstura (albo jej fragment) z kolorem ARGB. */
    public static void texture(DrawContext c, Identifier id, int x, int y, float u, float v, int w, int h, int texW, int texH, int color) {
        //#if MC >= 1.21.6
        //$$ c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, texW, texH, color);
        //#elseif MC >= 1.21.2
        c.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, u, v, w, h, texW, texH, color);
        //#else
        //$$ tint(c, color);
        //$$ c.drawTexture(id, x, y, u, v, w, h, texW, texH);
        //$$ untint(c);
        //#endif
    }

    /** Sprite z atlasu GUI (np. "hud/heart/full"). */
    public static void guiSprite(DrawContext c, Identifier id, int x, int y, int w, int h) {
        //#if MC >= 1.21.6
        //$$ c.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
        //#elseif MC >= 1.21.2
        c.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, w, h);
        //#else
        //$$ com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        //$$ c.drawGuiTexture(id, x, y, w, h);
        //#endif
    }

    public static void guiSprite(DrawContext c, Identifier id, int x, int y, int w, int h, int color) {
        //#if MC >= 1.21.6
        //$$ c.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, id, x, y, w, h, color);
        //#elseif MC >= 1.21.2
        c.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, w, h, color);
        //#else
        //$$ tint(c, color);
        //$$ c.drawGuiTexture(id, x, y, w, h);
        //$$ untint(c);
        //#endif
    }

    /** Sprite rozciągnięty na prostokąt (np. ikonka efektu mikstury). */
    public static void spriteStretched(DrawContext c, Sprite sprite, int x, int y, int w, int h) {
        //#if MC >= 1.21.6
        //$$ c.drawSpriteStretched(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
        //#elseif MC >= 1.21.2
        c.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, x, y, w, h);
        //#else
        //$$ c.drawSprite(x, y, 0, w, h, sprite);
        //#endif
    }

    //#if MC < 1.21.2
    //$$ private static void tint(DrawContext c, int color) {
    //$$     com.mojang.blaze3d.systems.RenderSystem.enableBlend();
    //$$     c.setShaderColor(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f);
    //$$ }
    //$$
    //$$ private static void untint(DrawContext c) {
    //$$     c.setShaderColor(1f, 1f, 1f, 1f);
    //$$ }
    //#endif

    /** Tekstura z obrazka w pamięci (ikonki serwerów, modów). */
    public static net.minecraft.client.texture.NativeImageBackedTexture nativeTexture(net.minecraft.client.texture.NativeImage img) {
        //#if MC >= 1.21.5
        //$$ return new net.minecraft.client.texture.NativeImageBackedTexture(() -> "zorza", img);
        //#else
        return new net.minecraft.client.texture.NativeImageBackedTexture(img);
        //#endif
    }

    /** Ułamek ticka do płynnej animacji (w świecie). */
    public static float worldDelta(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        //#if MC >= 1.21.5
        //$$ return ctx.tickCounter().getTickProgress(true);
        //#elseif MC >= 1.21
        return ctx.tickCounter().getTickDelta(true);
        //#else
        //$$ return ctx.tickDelta();
        //#endif
    }

    /** Ułamek ticka do płynnej animacji (HUD). */
    public static float frameDelta(net.minecraft.client.MinecraftClient mc) {
        //#if MC >= 1.21.5
        //$$ return mc.getRenderTickCounter().getTickProgress(true);
        //#elseif MC >= 1.21
        return mc.getRenderTickCounter().getTickDelta(true);
        //#else
        //$$ return mc.getTickDelta();
        //#endif
    }

    public static float lastBodyYaw(net.minecraft.entity.LivingEntity e) {
        //#if MC >= 1.21.5
        //$$ return e.lastBodyYaw;
        //#else
        return e.prevBodyYaw;
        //#endif
    }

    /** Ikonka efektu mikstury. */
    public static void effectIcon(DrawContext c, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int x, int y, int size) {
        //#if MC >= 1.21.6
        //$$ effect.getKey().ifPresent(k -> texture(c, Ids.of(k.getValue().getNamespace(), "textures/mob_effect/" + k.getValue().getPath() + ".png"),
        //$$         x, y, 0f, 0f, size, size, size, size, -1));
        //#else
        Sprite sprite = net.minecraft.client.MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(effect);
        if (sprite != null) spriteStretched(c, sprite, x, y, size, size);
        //#endif
    }
}
