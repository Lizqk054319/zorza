package pl.zorza.client.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.fx.MotionBlurRenderer;

@Mixin(GameRenderer.class)
public class GameRendererBlurMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
            shift = At.Shift.AFTER))
    private void zorza$motionBlur(CallbackInfo ci) {
        MotionBlurRenderer.afterWorld();
    }
}
