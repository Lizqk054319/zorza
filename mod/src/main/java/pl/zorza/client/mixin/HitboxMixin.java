package pl.zorza.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.VertexConsumer;
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
    // argumenty bierzemy przez @Local – metoda ma inne parametry w różnych wersjach gry
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void zorza$hitbox(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices,
                                     @Local(argsOnly = true) VertexConsumer vertices, @Local(argsOnly = true) Entity entity,
                                     @Local(argsOnly = true, ordinal = 0) float tickDelta) {
        CustomHitboxes h = Modules.CUSTOM_HITBOXES;
        if (h == null || !h.enabled) return;
        ci.cancel();
        if (h.playersOnly.value && !(entity instanceof PlayerEntity)) return;
        int c = h.color.value;
        float a = (float) h.opacity.value / 100f;
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        //#if MC >= 1.21.2
        net.minecraft.client.render.VertexRendering.drawBox(matrices, vertices, box, r, g, b, a);
        //#else
        //$$ net.minecraft.client.render.WorldRenderer.drawBox(matrices, vertices, box, r, g, b, a);
        //#endif
        if (h.lookLine.value) {
            Vec3d look = entity.getRotationVec(tickDelta).multiply(2.0);
            float eye = entity.getStandingEyeHeight();
            MatrixStack.Entry e = matrices.peek();
            Vec3d n = look.normalize();
            //#if MC >= 1.21
            vertices.vertex(e, 0, eye, 0).color(0f, 0f, 1f, 1f).normal(e, (float) n.x, (float) n.y, (float) n.z);
            vertices.vertex(e, (float) look.x, eye + (float) look.y, (float) look.z).color(0f, 0f, 1f, 1f)
                    .normal(e, (float) n.x, (float) n.y, (float) n.z);
            //#else
            //$$ vertices.vertex(e.getPositionMatrix(), 0, eye, 0).color(0f, 0f, 1f, 1f).normal(e.getNormalMatrix(), (float) n.x, (float) n.y, (float) n.z).next();
            //$$ vertices.vertex(e.getPositionMatrix(), (float) look.x, eye + (float) look.y, (float) look.z).color(0f, 0f, 1f, 1f)
            //$$         .normal(e.getNormalMatrix(), (float) n.x, (float) n.y, (float) n.z).next();
            //#endif
        }
    }
}
