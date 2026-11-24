package pl.zorza.client.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;

/** Skala: tab list (ustawienia w edytorze HUD -> Tab & Scoreboard). */
@Mixin(PlayerListHud.class)
public class PlayerListScaleMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void zorza$start(DrawContext ctx, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        float s = Modules.HUD_SCALE == null ? 1f : Modules.HUD_SCALE.tab();
        ctx.getMatrices().push();
        if (s != 1f) {
            float ax = width / 2f, ay = 10f;
            ctx.getMatrices().translate(ax, ay, 0);
            ctx.getMatrices().scale(s, s, 1f);
            ctx.getMatrices().translate(-ax, -ay, 0);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void zorza$end(DrawContext ctx, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        ctx.getMatrices().pop();
    }
}
