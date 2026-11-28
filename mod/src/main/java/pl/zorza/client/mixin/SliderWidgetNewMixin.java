//#file-if MC >= 1.21.11
package pl.zorza.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.zorza.client.gui.VanillaStyle;
import pl.zorza.client.modules.MenuSettings;

/** Suwaki w stylu Zory – 1.21.11+. */
@Mixin(SliderWidget.class)
public abstract class SliderWidgetNewMixin {
    @Shadow
    protected double value;

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void zorza$parts(DrawContext ctx, RenderPipeline layer, Identifier tex, int x, int y, int w, int h, int color) {
        if (!MenuSettings.styled()) {
            ctx.drawGuiTexture(layer, tex, x, y, w, h, color);
            return;
        }
        if (tex.getPath().contains("handle")) return;
        ClickableWidget self = (ClickableWidget) (Object) this;
        float a = ((color >>> 24) & 255) / 255f;
        VanillaStyle.slider(ctx, self, self.getX(), self.getY(), self.getWidth(), self.getHeight(), value, self.active, self.isSelected(), a);
        VanillaStyle.label(ctx, self.getMessage(), self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                ((int) (a * 255) << 24) | (self.active ? 0xFFFFFF : 0xA0A0A0));
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/SliderWidget;drawTextWithMargin(Lnet/minecraft/client/font/DrawnTextConsumer;Lnet/minecraft/text/Text;I)V"))
    private void zorza$text(SliderWidget self, net.minecraft.client.font.DrawnTextConsumer out, net.minecraft.text.Text text, int margin) {
        if (!MenuSettings.styled()) ((ClickableWidgetNewInvoker) self).zorza$drawTextWithMargin(out, text, margin);
    }
}
