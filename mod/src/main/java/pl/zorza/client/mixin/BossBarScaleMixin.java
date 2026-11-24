package pl.zorza.client.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;
import net.minecraft.client.gui.hud.BossBarHud;

/** Skala: boss bar (ustawienia w edytorze HUD -> Tab & Scoreboard). */
@Mixin(BossBarHud.class)
public class BossBarScaleMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void zorza$start(DrawContext ctx, CallbackInfo ci) {
        float s = Modules.HUD_SCALE == null ? 1f : Modules.HUD_SCALE.bossbar();
        ctx.getMatrices().push();
        if (s != 1f) {
            float ax = ctx.getScaledWindowWidth() / 2f, ay = 0f;
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().scale(s, s, 1f);
            ctx.getMatrices().translate(-ax, -ay, 0);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void zorza$end(DrawContext ctx, CallbackInfo ci) {
        ctx.getMatrices().pop();
    }
}
