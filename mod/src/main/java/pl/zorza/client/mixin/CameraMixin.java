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

    //#if MC >= 1.21
    @Shadow
    protected abstract void moveBy(float x, float y, float z);

    @Shadow
    protected abstract float clipToSpace(float distance);
    //#else
    //$$ @Shadow
    //$$ protected abstract void moveBy(double x, double y, double z);
    //$$
    //$$ @Shadow
    //$$ protected abstract double clipToSpace(double distance);
    //#endif

    @Inject(method = "update", at = @At("TAIL"))
    private void zorza$freelook(BlockView area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Modules.FREELOOK == null || !Modules.FREELOOK.active || entity == null) return;
        setRotation(Modules.FREELOOK.yaw, Modules.FREELOOK.pitch);
        net.minecraft.util.math.Vec3d lp = entity.getLerpedPos(tickDelta);
        setPos(lp.x, lp.y + MathHelper.lerp(tickDelta, lastCameraY, cameraY), lp.z);
        if (thirdPerson) {
            float scale = entity instanceof LivingEntity le ? le.getScale() : 1f;
            //#if MC >= 1.21
            moveBy(-clipToSpace(4f * scale), 0f, 0f);
            //#else
            //$$ moveBy(-clipToSpace(4.0 * scale), 0.0, 0.0);
            //#endif
        }
    }
}
