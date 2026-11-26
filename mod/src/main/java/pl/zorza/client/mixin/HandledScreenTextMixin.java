package pl.zorza.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.zorza.client.Packs;

/** Przy ciemnym ekwipunku ciemnoszare napisy (np. "Crafting", "Inventory") stają się jasne. */
@Mixin(HandledScreen.class)
public class HandledScreenTextMixin {
    //#if MC >= 1.21.6
    //$$ @Redirect(method = "drawForeground", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"))
    //$$ private void zorza$text(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
    //$$     if ((color & 0xFFFFFF) == 0x404040 && pl.zorza.client.gui.DarkGui.on()) color = 0xFFD8D8E0;
    //$$     ctx.drawText(tr, text, x, y, color, shadow);
    //$$ }
    //#else
    @Redirect(method = "drawForeground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"))
    private int zorza$text(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        if ((color & 0xFFFFFF) == 0x404040 && pl.zorza.client.gui.DarkGui.on()) color = 0xFFD8D8E0;
        return ctx.drawText(tr, text, x, y, color, shadow);
    }
    //#endif
}
