package pl.zorza.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.Modules;

@Mixin(HeldItemRenderer.class)
public class HeldItemMixin {
    //#if MC >= 1.21.9
    //$$ // 1.21.9+: renderItem rysuje przez kolejkę poleceń – wystarczy nazwa metody (wołana jest tylko jedna wersja)
    //$$ private static final String RENDER_ITEM = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem";
    //#elseif MC >= 1.21.5
    //$$ private static final String RENDER_ITEM = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
    //#elseif MC >= 1.21.2
    private static final String RENDER_ITEM = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
    //#else
    //$$ private static final String RENDER_ITEM = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
    //#endif

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
            target = RENDER_ITEM))
    private void zorza$viewModel(CallbackInfo ci, @Local(argsOnly = true) AbstractClientPlayerEntity player,
                                 @Local(argsOnly = true) Hand hand, @Local(argsOnly = true) MatrixStack matrices) {
        if (Modules.VIEW_MODEL == null || !Modules.VIEW_MODEL.enabled) return;
        boolean main = hand == Hand.MAIN_HAND;
        Arm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        Modules.VIEW_MODEL.apply(matrices, main, arm == Arm.RIGHT);
    }
}
