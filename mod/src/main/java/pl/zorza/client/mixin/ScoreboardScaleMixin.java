package pl.zorza.client.mixin;

import pl.zorza.client.compat.M;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;

/** Skala: scoreboard (ustawienia w edytorze HUD -> Tab & Scoreboard). */
@Mixin(InGameHud.class)
public class ScoreboardScaleMixin {
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"))
    private void zorza$start(DrawContext ctx, ScoreboardObjective objective, CallbackInfo ci) {
        float s = Modules.HUD_SCALE == null ? 1f : Modules.HUD_SCALE.scoreboard();
        M.push(ctx);
        if (s != 1f) {
            float ax = ctx.getScaledWindowWidth(), ay = ctx.getScaledWindowHeight() / 2f;
            M.translate(ctx, ax, ay);
            M.scale(ctx, s, s);
            M.translate(ctx, -ax, -ay);
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"))
    private void zorza$end(DrawContext ctx, ScoreboardObjective objective, CallbackInfo ci) {
        M.pop(ctx);
    }
}
