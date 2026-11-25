package pl.zorza.client.mixin;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;
import pl.zorza.client.modules.CustomHitboxes;

@Mixin(EntityRenderDispatcher.class)
public class HitboxMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void zorza$hitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta,
                                     float red, float green, float blue, CallbackInfo ci) {
        CustomHitboxes h = Modules.CUSTOM_HITBOXES;
        if (h == null || !h.enabled) return;
        ci.cancel();
        if (h.playersOnly.value && !(entity instanceof PlayerEntity)) return;
        int c = h.color.value;
        float a = (float) h.opacity.value / 100f;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        VertexRendering.drawBox(matrices, vertices, box, ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, a);
        if (h.lookLine.value) {
            Vec3d look = entity.getRotationVec(tickDelta);
            VertexRendering.drawVector(matrices, vertices, new org.joml.Vector3f(0, entity.getStandingEyeHeight(), 0),
                    look.multiply(2.0), 0xFF0000FF);
        }
    }
}
