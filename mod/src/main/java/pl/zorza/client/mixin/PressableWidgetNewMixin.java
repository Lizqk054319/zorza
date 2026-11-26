//#file-if MC >= 1.21.11
package pl.zorza.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.gui.VanillaStyle;
import pl.zorza.client.modules.MenuSettings;

/** Przyciski w stylu Zory – 1.21.11+ (tło w drawButton, napis w drawLabel). */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetNewMixin {
    @Redirect(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void zorza$background(DrawContext ctx, RenderPipeline layer, Identifier tex, int x, int y, int w, int h, int color) {
        if (!MenuSettings.styled()) {
            ctx.drawGuiTexture(layer, tex, x, y, w, h, color);
            return;
        }
        ClickableWidget self = (ClickableWidget) (Object) this;
        float a = ((color >>> 24) & 255) / 255f;
        VanillaStyle.button(ctx, self, x, y, w, h, self.active, self.isSelected(), a);
        if (self.getClass().getName().contains("IconOnly") || self.getClass().getSimpleName().contains("IconOnly")) return;   // sama ikonka – bez napisu
        int tc = self.active ? 0xFFFFFF : 0xA0A0A0;
        VanillaStyle.label(ctx, self.getMessage().getString(), self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                ((int) (a * 255) << 24) | tc);
    }

    @Inject(method = "drawLabel", at = @At("HEAD"), cancellable = true)
    private void zorza$label(CallbackInfo ci) {
        if (MenuSettings.styled()) ci.cancel();          // napis narysowaliśmy razem z tłem
    }
}
