//#file-if MC < 1.21.4
package pl.zorza.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test dymny dla wersji bez klientowych gametestów Fabric API (CI, tylko z -Dzorza.smoke=folder).
 * Gra startuje, wszystkie mixiny zostają sprawdzone, robimy zrzuty menu Zory i gra się zamyka.
 */
public final class SmokeTest {
    private static int ticks = 0, step = 0;

    private SmokeTest() {
    }

    public static void init() {
        String dir = System.getProperty("zorza.smoke");
        if (dir == null) return;
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (step == 0 && !(mc.currentScreen instanceof TitleScreen)) return;
            ticks++;
            try {
                if (step == 0 && ticks > 60) {
                    MixinEnvironment.getCurrentEnvironment().audit();   // wywala się, jeśli jakiś mixin nie pasuje
                    shot(mc, "01-title");
                    mc.setScreen(new pl.zorza.client.gui.ZoraModsScreen(mc.currentScreen));
                    next();
                } else if (step == 1 && ticks > 30) {
                    shot(mc, "02-zora-mods");
                    mc.setScreen(new pl.zorza.client.gui.ModMenuScreen(null));
                    next();
                } else if (step == 2 && ticks > 30) {
                    shot(mc, "03-zora-menu");
                    mc.setScreen(new pl.zorza.client.gui.HudEditorScreen(true));
                    next();
                } else if (step == 3 && ticks > 30) {
                    shot(mc, "04-hud-editor");
                    Files.writeString(Path.of(dir, "result.txt"), "SMOKE OK");
                    ZorzaClient.LOG.info("SMOKE OK");
                    mc.scheduleStop();
                    next();
                }
            } catch (Throwable t) {
                ZorzaClient.LOG.error("SMOKE FAIL", t);
                try {
                    Files.createDirectories(Path.of(dir));
                    Files.writeString(Path.of(dir, "result.txt"), "SMOKE FAIL " + t);
                } catch (Exception ignored) {
                }
                mc.scheduleStop();
                step = 99;
            }
        });
    }

    private static void next() {
        step++;
        ticks = 0;
    }

    private static void shot(MinecraftClient mc, String name) throws Exception {
        Files.createDirectories(Path.of(System.getProperty("zorza.smoke")));
        ScreenshotRecorder.saveScreenshot(new File(System.getProperty("zorza.smoke")), name + ".png",
                mc.getFramebuffer(), t -> { });
    }
}
