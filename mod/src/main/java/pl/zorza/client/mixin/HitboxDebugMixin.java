//#file-if MC >= 1.21.11
package pl.zorza.client.mixin;

import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;
import pl.zorza.client.modules.CustomHitboxes;

/** 1.21.11+: hitboxy rysuje nowy system debugowania – działa opcja "tylko gracze". */
@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxDebugMixin {
    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true)
    private void zorza$who(Entity entity, float tickDelta, boolean server, CallbackInfo ci) {
        CustomHitboxes h = Modules.CUSTOM_HITBOXES;
        if (h != null && h.enabled && h.playersOnly.value && !(entity instanceof PlayerEntity)) ci.cancel();
    }
}
