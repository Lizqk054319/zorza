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
        //#elseif MC >= 1.20.2
        //$$ com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        //$$ c.drawGuiTexture(id, x, y, w, h);
        //#endif
    }

    public static void guiSprite(DrawContext c, Identifier id, int x, int y, int w, int h, int color) {
        //#if MC >= 1.21.6
        //$$ c.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, id, x, y, w, h, color);
        //#elseif MC >= 1.21.2
        c.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, w, h, color);
        //#elseif MC >= 1.20.2
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

    //#if MC < 1.21.9
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
    //#endif

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
    @SuppressWarnings("unchecked")
    public static void effectIcon(DrawContext c, Object effectObj, int x, int y, int size) {
        //#if MC >= 1.20.5
        var effect = (net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>) effectObj;
        //#else
        //$$ var effect = (net.minecraft.entity.effect.StatusEffect) effectObj;
        //#endif
        //#if MC >= 1.21.6
        //$$ effect.getKey().ifPresent(k -> texture(c, Ids.of(k.getValue().getNamespace(), "textures/mob_effect/" + k.getValue().getPath() + ".png"),
        //$$         x, y, 0f, 0f, size, size, size, size, -1));
        //#else
        Sprite sprite = net.minecraft.client.MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(effect);
        if (sprite != null) spriteStretched(c, sprite, x, y, size, size);
        //#endif
    }

    /** Styl z daną czcionką (od 1.21.9 czcionka to StyleSpriteSource). */
    public static net.minecraft.text.Style font(net.minecraft.text.Style st, Identifier font) {
        //#if MC >= 1.21.9
        //$$ return st.withFont(new net.minecraft.text.StyleSpriteSource.Font(font));
        //#else
        return st.withFont(font);
        //#endif
    }

    /** Pozycja kamery w świecie. */
    public static net.minecraft.util.math.Vec3d camPos(net.minecraft.client.render.Camera cam) {
        //#if MC >= 1.21.11
        //$$ return cam.getCameraPos();
        //#else
        return cam.getPos();
        //#endif
    }

    public static net.minecraft.client.render.RenderLayer debugQuads() {
        //#if MC >= 1.21.11
        //$$ return net.minecraft.client.render.RenderLayers.debugQuads();
        //#else
        return net.minecraft.client.render.RenderLayer.getDebugQuads();
        //#endif
    }

    public static net.minecraft.client.render.RenderLayer textLayer(Identifier tex) {
        //#if MC >= 1.21.11
        //$$ return net.minecraft.client.render.RenderLayers.text(tex);
        //#else
        return net.minecraft.client.render.RenderLayer.getText(tex);
        //#endif
    }

    public static net.minecraft.client.sound.PositionedSoundInstance uiSound(net.minecraft.sound.SoundEvent ev, float pitch, float volume) {
        //#if MC >= 1.21.11
        //$$ return net.minecraft.client.sound.PositionedSoundInstance.ui(ev, pitch, volume);
        //#else
        return net.minecraft.client.sound.PositionedSoundInstance.master(ev, pitch, volume);
        //#endif
    }

    /** Ping serwera (od 1.21.11 metoda chce jeszcze "backend" sieci – bierzemy go przez refleksję). */
    public static void ping(net.minecraft.client.network.MultiplayerServerListPinger pinger, net.minecraft.client.network.ServerInfo info) throws Exception {
        //#if MC >= 1.21.11
        //$$ for (java.lang.reflect.Method m : pinger.getClass().getMethods()) {
        //$$     if (!m.getName().equals("add") || m.getParameterCount() != 4) continue;
        //$$     Class<?> nb = m.getParameterTypes()[3];
        //$$     Object backend = null;
        //$$     for (java.lang.reflect.Field f : nb.getFields())
        //$$         if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && nb.isAssignableFrom(f.getType())) { backend = f.get(null); break; }
        //$$     if (backend == null) for (java.lang.reflect.Method f : nb.getMethods())
        //$$         if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && nb.isAssignableFrom(f.getReturnType())) {
        //$$             if (f.getParameterCount() == 0) { backend = f.invoke(null); break; }
        //$$             if (f.getParameterCount() == 1 && f.getParameterTypes()[0] == boolean.class) { backend = f.invoke(null, false); break; }
        //$$         }
        //$$     m.invoke(pinger, info, (Runnable) () -> {}, (Runnable) () -> {}, backend);
        //$$     return;
        //$$ }
        //#elseif MC >= 1.20.5
        pinger.add(info, () -> {}, () -> {});
        //#else
        //$$ pinger.add(info, () -> {});
        //#endif
    }

    /** Czy tekst ma coś, czego nasza czcionka nie odda: własna czcionka serwera, kolory, znaki spoza alfabetu łacińskiego. */
    public static boolean special(net.minecraft.text.Text text) {
        java.util.Optional<Boolean> r = text.visit((style, str) -> {
            if (style.getColor() != null || style.isObfuscated()) return java.util.Optional.of(true);
            if (customFont(style)) return java.util.Optional.of(true);
            for (int i = 0; i < str.length(); ) {
                int cp = str.codePointAt(i);
                i += Character.charCount(cp);
                if (cp > 0x024F && !(cp >= 0x2000 && cp <= 0x206F) && cp != 0x20AC) return java.util.Optional.of(true);
            }
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return r.isPresent();
    }

    private static boolean customFont(net.minecraft.text.Style style) {
        //#if MC >= 1.21.9
        //$$ var f = style.getFont();
        //$$ return f != null && !(f instanceof net.minecraft.text.StyleSpriteSource.Font ff && ff.id().equals(Ids.mc("default")));
        //#else
        Identifier f = style.getFont();
        return f != null && !f.equals(Ids.mc("default"));
        //#endif
    }
}
