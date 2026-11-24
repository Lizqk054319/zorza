package pl.zorza.client.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.zorza.client.Modules;

@Mixin(World.class)
public class WeatherMixin {
    @Inject(method = {"getRainGradient", "getThunderGradient"}, at = @At("HEAD"), cancellable = true)
    private void zorza$clear(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientWorld && Modules.CLEAR_WEATHER != null && Modules.CLEAR_WEATHER.enabled) cir.setReturnValue(0f);
    }
}
