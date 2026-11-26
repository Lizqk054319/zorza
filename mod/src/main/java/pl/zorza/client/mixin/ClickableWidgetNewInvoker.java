//#file-if MC >= 1.21.11
package pl.zorza.client.mixin;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetNewInvoker {
    @Invoker("drawTextWithMargin")
    void zorza$drawTextWithMargin(net.minecraft.client.font.DrawnTextConsumer out, net.minecraft.text.Text text, int margin);
}
