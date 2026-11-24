package pl.zorza.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.zorza.client.Modules;

/** Moduł Gamma: podmienia jasność tylko przy liczeniu oświetlenia (nie zapisuje jej w options.txt). */
@Mixin(LightmapTextureManager.class)
public class LightmapMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object zorza$gamma(SimpleOption<?> option) {
        Object v = option.getValue();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (Modules.GAMMA != null && Modules.GAMMA.enabled && mc.options != null && option == mc.options.getGamma()) {
            return Modules.GAMMA.factor();
        }
        return v;
    }
}
