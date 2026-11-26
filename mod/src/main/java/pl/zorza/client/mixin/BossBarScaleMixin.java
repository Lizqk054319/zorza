package pl.zorza.client.mixin;

import pl.zorza.client.compat.M;
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
        M.push(ctx);
        if (s != 1f) {
            float ax = ctx.getScaledWindowWidth() / 2f, ay = 0f;
            M.translate(ctx, ax, ay);
            M.scale(ctx, s, s);
            M.translate(ctx, -ax, -ay);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void zorza$end(DrawContext ctx, CallbackInfo ci) {
        M.pop(ctx);
    }
}
