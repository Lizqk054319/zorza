package pl.zorza.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TrueTypeFontLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;
import pl.zorza.client.ZorzaClient;

/**
 * Czcionka "Zora Font" (cała gra) rasteryzowana dokładnie w skali GUI – ostre, nierozmazane napisy.
 * Rozpoznajemy ją po rozmiarze 8.6 z default.json paczki.
 */
@Mixin(TrueTypeFontLoader.class)
public abstract class TrueTypeFontLoaderMixin {
    @Shadow
    @Final
    private float size;

    @Shadow
    @Final
    private float oversample;

    private static boolean zorza$logged = false;

    @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/client/font/TrueTypeFontLoader;oversample:F", opcode = Opcodes.GETFIELD), require = 0)
    private float zorza$oversample(TrueTypeFontLoader self) {
        float base = oversample;
        if (Math.abs(size - 8.6f) > 0.001f) return base;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return base;
        float k = (float) Math.max(1, Math.min(8, mc.getWindow().getScaleFactor()));
        if (!zorza$logged) {
            zorza$logged = true;
            ZorzaClient.LOG.info("Zora Font: oversample {}", k);
        }
        return k;
    }
}
