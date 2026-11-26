package pl.zorza.client.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.fx.MotionBlurRenderer;

@Mixin(GameRenderer.class)
public class GameRendererBlurMixin {
    //#if MC >= 1.21
    private static final String RENDER_WORLD = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V";
    //#elseif MC >= 1.20.5
    //$$ private static final String RENDER_WORLD = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJ)V";
    //#else
    //$$ private static final String RENDER_WORLD = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V";
    //#endif

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = RENDER_WORLD,
            shift = At.Shift.AFTER))
    private void zorza$motionBlur(CallbackInfo ci) {
        MotionBlurRenderer.afterWorld();
    }
}
