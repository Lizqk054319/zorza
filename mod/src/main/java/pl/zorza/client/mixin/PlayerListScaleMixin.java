package pl.zorza.client.mixin;

import pl.zorza.client.compat.M;
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
        M.push(ctx);
        if (s != 1f) {
            float ax = width / 2f, ay = 10f;
            M.translate(ctx, ax, ay);
            M.scale(ctx, s, s);
            M.translate(ctx, -ax, -ay);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void zorza$end(DrawContext ctx, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        M.pop(ctx);
    }
}
