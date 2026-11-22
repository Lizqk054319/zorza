package pl.zorza.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void zorza$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Modules.CROSSHAIR != null && Modules.CROSSHAIR.enabled) {
            ci.cancel();
            Modules.CROSSHAIR.drawCrosshair(context);
        }
    }
}
