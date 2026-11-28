//#file-if MC >= 1.21.9
package pl.zorza.client.mixin;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.command.DebugHitboxCommandRenderer;
import net.minecraft.client.render.entity.state.EntityHitbox;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
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

/** Custom Hitboxes dla 1.21.9+ (hitboxy rysowane przez kolejkę poleceń). Hitboxy włączasz F3+B. */
@Mixin(DebugHitboxCommandRenderer.class)
public class HitboxCommandMixin {
    @Inject(method = "renderDebugHitbox", at = @At("HEAD"), cancellable = true)
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
            VertexRendering.drawBox(m.peek(), vc, bb, r, g, b, a);
        }
        if (h.lookLine.value) {
            VertexRendering.drawVector(m, vc, new Vector3f(0f, eye, 0f),
                    new Vec3d(hb.viewX(), hb.viewY(), hb.viewZ()).multiply(2.0), 0xFF0000FF);
        }
    }
}
