package pl.zorza.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Liczby na przedmiotach (np. 25 mięsa) zostają zwykłą, pikselową czcionką Minecrafta. */
@Mixin(DrawContext.class)
public abstract class ItemCountMixin {
    private static final Identifier VANILLA = Identifier.of("zorza", "vanilla");

    private static Text zorza$v(String s) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(VANILLA));
    }

    @Redirect(method = "drawStackCount",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"),
            require = 1)
    private int zorza$count(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        return ctx.drawText(tr, zorza$v(s), x, y, color, shadow);
    }

    @Redirect(method = "drawStackCount",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"),
            require = 1)
    private int zorza$width(TextRenderer tr, String s) {
        return tr.getWidth(zorza$v(s));
    }
}
