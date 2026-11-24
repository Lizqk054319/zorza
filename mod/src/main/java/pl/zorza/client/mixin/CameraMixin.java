package pl.zorza.client.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pl.zorza.client.Modules;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0), index = 0)
    private float zorza$yaw(float yaw) {
        return Modules.FREELOOK != null && Modules.FREELOOK.active ? Modules.FREELOOK.yaw : yaw;
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0), index = 1)
    private float zorza$pitch(float pitch) {
        return Modules.FREELOOK != null && Modules.FREELOOK.active ? Modules.FREELOOK.pitch : pitch;
    }
}
