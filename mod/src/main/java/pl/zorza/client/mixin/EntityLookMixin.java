package pl.zorza.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;

@Mixin(Entity.class)
public class EntityLookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void zorza$freelook(double dx, double dy, CallbackInfo ci) {
        if ((Object) this == MinecraftClient.getInstance().player && Modules.FREELOOK != null && Modules.FREELOOK.onMouse(dx, dy)) {
            ci.cancel();
        }
    }
}
