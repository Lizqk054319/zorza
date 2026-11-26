package pl.zorza.client.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.ZorzaClient;

@Mixin(Mouse.class)
public class MouseMixin {
    //#if MC >= 1.21.9
    //$$ @Inject(method = "onMouseButton", at = @At("HEAD"))
    //$$ private void zorza$click(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
    //$$     if (action == 1) ZorzaClient.onClick(input.button());
    //$$ }
    //#else
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void zorza$click(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1) ZorzaClient.onClick(button);
    }
    //#endif
}
