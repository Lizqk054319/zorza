//#file-if MC >= 1.21.5 && MC < 1.21.9
package pl.zorza.client.mixin;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityHitbox;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;
import pl.zorza.client.modules.CustomHitboxes;

/** Custom Hitboxes dla 1.21.5–1.21.8 (hitboxy rysowane z "render state", bez dostępu do encji). */
@Mixin(EntityRenderDispatcher.class)
public class HitboxStateMixin {
    @Inject(method = "renderHitboxes(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/entity/state/EntityHitboxAndView;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true)
    private void zorza$who(MatrixStack m, EntityRenderState state, EntityHitboxAndView hb, VertexConsumerProvider vcp, CallbackInfo ci) {
        CustomHitboxes h = Modules.CUSTOM_HITBOXES;
        if (h != null && h.enabled && h.playersOnly.value && !(state instanceof PlayerEntityRenderState)) ci.cancel();
    }

    @Inject(method = "renderHitboxes(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/state/EntityHitboxAndView;Lnet/minecraft/client/render/VertexConsumer;F)V",
            at = @At("HEAD"), cancellable = true)
    private static void zorza$draw(MatrixStack m, EntityHitboxAndView hb, VertexConsumer vc, float eye, CallbackInfo ci) {
        CustomHitboxes h = Modules.CUSTOM_HITBOXES;
        if (h == null || !h.enabled) return;
        ci.cancel();
        int c = h.color.value;
        float a = (float) h.opacity.value / 100f;
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        for (EntityHitbox box : hb.hitboxes()) {
            if (box.red() == 1f && box.green() == 0f && box.blue() == 0f) continue;   // czerwona linia oczu – bez niej
            Box bb = new Box(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1())
                    .offset(box.offsetX(), box.offsetY(), box.offsetZ());
            VertexRendering.drawBox(m, vc, bb, r, g, b, a);
        }
        if (h.lookLine.value) {
            VertexRendering.drawVector(m, vc, new Vector3f(0f, eye, 0f),
                    new Vec3d(hb.viewX(), hb.viewY(), hb.viewZ()).multiply(2.0), 0xFF0000FF);
        }
    }
}
