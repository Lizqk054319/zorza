package pl.zorza.client.mixin;

import pl.zorza.client.compat.Ids;

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
    private static final Identifier VANILLA = Ids.of("zorza", "vanilla");

    //#if MC >= 1.21.2
    private static final String COUNT = "drawStackCount";
    //#else
    //$$ private static final String COUNT = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V";
    //#endif

    private static Text zorza$v(String s) {
        return Text.literal(s).setStyle(pl.zorza.client.compat.Gfx.font(Style.EMPTY, VANILLA));
    }

    //#if MC >= 1.21.6
    //$$ @Redirect(method = COUNT,
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V"),
    //$$         require = 1)
    //$$ private void zorza$count(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
    //$$     ctx.drawText(tr, zorza$v(s), x, y, color, shadow);
    //$$ }
    //#else
    @Redirect(method = COUNT,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"),
            require = 1)
    private int zorza$count(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        return ctx.drawText(tr, zorza$v(s), x, y, color, shadow);
    }
    //#endif

    @Redirect(method = COUNT,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"),
            require = 1)
    private int zorza$width(TextRenderer tr, String s) {
        return tr.getWidth(zorza$v(s));
    }
}
