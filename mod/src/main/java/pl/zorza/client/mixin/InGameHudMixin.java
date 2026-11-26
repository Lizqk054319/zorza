package pl.zorza.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void zorza$crosshair(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) DrawContext context) {
        if (Modules.CROSSHAIR != null && Modules.CROSSHAIR.enabled) {
            ci.cancel();
            Modules.CROSSHAIR.drawCrosshair(context);
        }
    }

    //#if MC >= 1.20.5
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void zorza$armor(DrawContext context, PlayerEntity player, int i, int j, int k, int x, CallbackInfo ci) {
        if (Modules.HIDE_BARS != null && Modules.HIDE_BARS.hideArmor()) ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void zorza$food(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        if (Modules.HIDE_BARS != null && Modules.HIDE_BARS.hideHunger()) ci.cancel();
    }
    //#elseif MC >= 1.20.2
    //$$ // 1.20.2–1.20.4: zbroja i głód rysują się w renderStatusBars jako sprite'y "hud/armor_*" i "hud/food_*"
    //$$ @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    //$$ private void zorza$bars(DrawContext ctx, net.minecraft.util.Identifier tex, int x, int y, int w, int h) {
    //$$     String p = tex.getPath();
    //$$     if (Modules.HIDE_BARS != null && ((p.startsWith("hud/armor") && Modules.HIDE_BARS.hideArmor())
    //$$             || (p.startsWith("hud/food") && Modules.HIDE_BARS.hideHunger()))) return;
    //$$     ctx.drawGuiTexture(tex, x, y, w, h);
    //$$ }
    //#else
    //$$ // 1.20.1: wszystko z icons.png – zbroja to wiersz v=9, głód v=27
    //$$ @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"))
    //$$ private void zorza$bars(DrawContext ctx, net.minecraft.util.Identifier tex, int x, int y, int u, int v, int w, int h) {
    //$$     if (Modules.HIDE_BARS != null && ((v == 9 && Modules.HIDE_BARS.hideArmor()) || (v == 27 && Modules.HIDE_BARS.hideHunger()))) return;
    //$$     ctx.drawTexture(tex, x, y, u, v, w, h);
    //$$ }
    //#endif
}
