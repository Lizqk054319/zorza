package pl.zorza.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.gui.VanillaStyle;
import pl.zorza.client.modules.MenuSettings;

import java.util.function.Function;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin {
    //#if MC >= 1.21.2
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void zorza$background(DrawContext ctx, Function<Identifier, RenderLayer> layer, Identifier tex, int x, int y, int w, int h, int color) {
        if (!MenuSettings.styled()) {
            ctx.drawGuiTexture(layer, tex, x, y, w, h, color);
            return;
        }
        ClickableWidget self = (ClickableWidget) (Object) this;
        VanillaStyle.button(ctx, self, x, y, w, h, self.active, self.isSelected(), ((color >>> 24) & 255) / 255f);
    }
    //#else
    //$$ @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    //$$ private void zorza$background(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
    //$$     if (!MenuSettings.styled()) {
    //$$         ctx.drawGuiTexture(tex, x, y, w, h);
    //$$         return;
    //$$     }
    //$$     ClickableWidget self = (ClickableWidget) (Object) this;
    //$$     VanillaStyle.button(ctx, self, x, y, w, h, self.active, self.isSelected(), com.mojang.blaze3d.systems.RenderSystem.getShaderColor()[3]);
    //$$ }
    //#endif

    @Inject(method = "drawMessage", at = @At("HEAD"), cancellable = true)
    private void zorza$message(DrawContext ctx, TextRenderer tr, int color, CallbackInfo ci) {
        if (!MenuSettings.styled()) return;
        ClickableWidget self = (ClickableWidget) (Object) this;
        VanillaStyle.label(ctx, self.getMessage().getString(), self.getX(), self.getY(), self.getWidth(), self.getHeight(), color);
        ci.cancel();
    }
}
