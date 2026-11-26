//#file-if MC < 1.21.11
package pl.zorza.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetInvoker {
    @Invoker("drawScrollableText")
    void zorza$drawScrollableText(DrawContext ctx, TextRenderer tr, int margin, int color);
}
