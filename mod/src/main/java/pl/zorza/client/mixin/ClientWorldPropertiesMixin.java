package pl.zorza.client.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.zorza.client.Modules;
import pl.zorza.client.modules.TimeChanger;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void zorza$time(CallbackInfoReturnable<Long> cir) {
        if (Modules.TIME_CHANGER != null && Modules.TIME_CHANGER.enabled) {
            long real = cir.getReturnValue();
            TimeChanger.realTime = real;
            cir.setReturnValue(real - Math.floorMod(real, 24000L) + Modules.TIME_CHANGER.ticks());
        }
    }
}
