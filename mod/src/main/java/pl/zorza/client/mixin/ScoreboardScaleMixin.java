package pl.zorza.client.mixin;

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
        ctx.getMatrices().push();
        if (s != 1f) {
            float ax = ctx.getScaledWindowWidth(), ay = ctx.getScaledWindowHeight() / 2f;
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().scale(s, s, 1f);
            ctx.getMatrices().translate(-ax, -ay, 0);
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"))
    private void zorza$end(DrawContext ctx, ScoreboardObjective objective, CallbackInfo ci) {
        ctx.getMatrices().pop();
    }
}
