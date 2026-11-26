package pl.zorza.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void zorza$armor(DrawContext context, PlayerEntity player, int i, int j, int k, int x, CallbackInfo ci) {
        if (Modules.HIDE_BARS != null && Modules.HIDE_BARS.hideArmor()) ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void zorza$food(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        if (Modules.HIDE_BARS != null && Modules.HIDE_BARS.hideHunger()) ci.cancel();
    }
}
