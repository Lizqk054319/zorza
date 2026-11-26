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

    //#if MC >= 1.21.6
    //$$ @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"))
    //$$ private void zorza$parts(DrawContext ctx, com.mojang.blaze3d.pipeline.RenderPipeline layer, Identifier tex, int x, int y, int w, int h, int color) {
    //$$     if (!MenuSettings.styled()) {
    //$$         ctx.drawGuiTexture(layer, tex, x, y, w, h, color);
    //$$         return;
    //$$     }
    //#elseif MC >= 1.21.2
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void zorza$parts(DrawContext ctx, Function<Identifier, RenderLayer> layer, Identifier tex, int x, int y, int w, int h, int color) {
        if (!MenuSettings.styled()) {
            ctx.drawGuiTexture(layer, tex, x, y, w, h, color);
            return;
        }
    //#else
    //$$ @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    //$$ private void zorza$parts(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
    //$$     int color = ((int) (com.mojang.blaze3d.systems.RenderSystem.getShaderColor()[3] * 255) << 24) | 0xFFFFFF;
    //$$     if (!MenuSettings.styled()) {
    //$$         ctx.drawGuiTexture(tex, x, y, w, h);
    //$$         return;
    //$$     }
    //#endif
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
