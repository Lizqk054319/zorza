package pl.zorza.client.mixin;

import net.minecraft.client.gui.widget.ScrollableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.zorza.client.SmoothScroll;

/** Płynne przewijanie list Minecrafta (serwery, światy, opcje...). */
@Mixin(ScrollableWidget.class)
public abstract class ScrollableWidgetMixin implements SmoothScroll.Access {
    @Shadow
    public abstract double getScrollY();

    @Shadow
    public abstract void setScrollY(double y);

    @Unique
    private double zorza$before;

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void zorza$scrollStart(double mx, double my, double h, double v, CallbackInfoReturnable<Boolean> cir) {
        zorza$before = getScrollY();
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void zorza$scrollEnd(double mx, double my, double h, double v, CallbackInfoReturnable<Boolean> cir) {
        double after = getScrollY();
        if (after != zorza$before && SmoothScroll.enabled()) {
            SmoothScroll.start(this, zorza$before, after);
            setScrollY(zorza$before);       // dojedziemy tam płynnie
        }
    }

    @Override
    public double zorza$get() { return getScrollY(); }

    @Override
    public void zorza$set(double y) { setScrollY(y); }
}
