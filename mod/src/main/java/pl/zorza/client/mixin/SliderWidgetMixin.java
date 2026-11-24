package pl.zorza.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.zorza.client.gui.VanillaStyle;
import pl.zorza.client.modules.MenuSettings;

import java.util.function.Function;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin {
    @Shadow
    protected double value;

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void zorza$parts(DrawContext ctx, Function<Identifier, RenderLayer> layer, Identifier tex, int x, int y, int w, int h, int color) {
        if (!MenuSettings.styled()) {
            ctx.drawGuiTexture(layer, tex, x, y, w, h, color);
            return;
        }
        if (tex.getPath().contains("handle")) return;          // uchwyt rysujemy razem z tłem
        ClickableWidget self = (ClickableWidget) (Object) this;
        VanillaStyle.slider(ctx, self, self.getX(), self.getY(), self.getWidth(), self.getHeight(), value, self.active,
                self.isSelected(), ((color >>> 24) & 255) / 255f);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/SliderWidget;drawScrollableText(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;II)V"))
    private void zorza$text(SliderWidget self, DrawContext ctx, TextRenderer tr, int margin, int color) {
        if (!MenuSettings.styled()) {
            ((ClickableWidgetInvoker) self).zorza$drawScrollableText(ctx, tr, margin, color);
            return;
        }
        VanillaStyle.label(ctx, self.getMessage().getString(), self.getX(), self.getY(), self.getWidth(), self.getHeight(), color);
    }
}
