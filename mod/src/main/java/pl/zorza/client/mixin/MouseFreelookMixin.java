package pl.zorza.client.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.zorza.client.Modules;

/** Freelook: ruch myszy obraca kamerę (360°), a nie postać. */
@Mixin(Mouse.class)
public class MouseFreelookMixin {
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void zorza$look(ClientPlayerEntity player, double dx, double dy) {
        if (Modules.FREELOOK != null && Modules.FREELOOK.onMouse(dx, dy)) return;
        player.changeLookDirection(dx, dy);
    }
}
