package pl.zorza.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.zorza.client.gui.ZoraHotbar;
import pl.zorza.client.modules.MenuSettings;

import java.util.function.Function;

/** Własny pasek przedmiotów (zaokrąglone sloty z obramówką) zamiast tekstury Minecrafta. */
@Mixin(InGameHud.class)
public class HotbarMixin {
    //#if MC >= 1.21.2
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"))
    private void zorza$hotbar(DrawContext ctx, Function<Identifier, RenderLayer> layer, Identifier tex, int x, int y, int w, int h) {
        if (!MenuSettings.hotbar() || !ZoraHotbar.draw(ctx, tex.getPath(), x, y, w, h)) {
            ctx.drawGuiTexture(layer, tex, x, y, w, h);
        }
    }
    //#else
    //$$ @Redirect(method = "renderHotbar", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    //$$ private void zorza$hotbar(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
    //$$     if (!MenuSettings.hotbar() || !ZoraHotbar.draw(ctx, tex.getPath(), x, y, w, h)) {
    //$$         ctx.drawGuiTexture(tex, x, y, w, h);
    //$$     }
    //$$ }
    //#endif
}
