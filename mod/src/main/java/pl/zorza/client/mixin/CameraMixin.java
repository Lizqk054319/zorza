package pl.zorza.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;

/**
 * Freelook: po zwykłym ustawieniu kamery ustawiamy ją jeszcze raz – z obrotem z Freelooka.
 * Działa nawet wtedy, gdy inny mod zmienia ustawianie kamery.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float lastCameraY;
    @Shadow
    private float cameraY;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void moveBy(float x, float y, float z);

    @Shadow
    protected abstract float clipToSpace(float distance);

    @Inject(method = "update", at = @At("TAIL"))
    private void zorza$freelook(BlockView area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Modules.FREELOOK == null || !Modules.FREELOOK.active || entity == null) return;
        setRotation(Modules.FREELOOK.yaw, Modules.FREELOOK.pitch);
        setPos(MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) + MathHelper.lerp(tickDelta, lastCameraY, cameraY),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        if (thirdPerson) {
            float scale = entity instanceof LivingEntity le ? le.getScale() : 1f;
            moveBy(-clipToSpace(4f * scale), 0f, 0f);
        }
    }
}
