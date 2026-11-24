package pl.zorza.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;

@Mixin(InGameOverlayRenderer.class)
public class FireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void zorza$lowFireStart(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices) {
        matrices.push();
        if (Modules.LOW_FIRE != null && Modules.LOW_FIRE.enabled) matrices.translate(0, -Modules.LOW_FIRE.height.value, 0);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void zorza$lowFireEnd(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices) {
        matrices.pop();
    }
}
