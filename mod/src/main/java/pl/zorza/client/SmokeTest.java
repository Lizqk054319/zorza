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
    private static int ticks = 0, step = 0, wait = 0;

    private SmokeTest() {
    }

    public static void init() {
        String dir = System.getProperty("zorza.smoke");
        if (dir == null) return;
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.getOverlay() != null) return;                          // jeszcze ładuje zasoby
            if (step == 0 && !(mc.currentScreen instanceof TitleScreen)) {
                if (++wait > 200 && mc.getOverlay() == null) mc.setScreen(new TitleScreen());   // np. ekran powitalny przy 1. starcie
                return;
            }
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
                    mc.options.pauseOnLostFocus = false;
                    net.minecraft.client.gui.screen.world.CreateWorldScreen.create(mc, new TitleScreen());
                    next();
                } else if (step == 4 && ticks > 60) {
                    // przycisk "Utwórz nowy świat"
                    boolean pressed = false;
                    if (mc.currentScreen != null) for (var el : mc.currentScreen.children()) {
                        if (el instanceof net.minecraft.client.gui.widget.ButtonWidget b && b.getMessage().getContent() instanceof net.minecraft.text.TranslatableTextContent tc
                                && tc.getKey().equals("selectWorld.create")) {
                            b.onPress();
                            pressed = true;
                            break;
                        }
                    }
                    if (!pressed) throw new IllegalStateException("Nie znaleziono przycisku tworzenia świata");
                    next();
                } else if (step == 5) {
                    if (mc.world != null && mc.player != null && mc.currentScreen == null && ticks > 100) {
                        for (var m : Modules.ALL) m.setEnabled(true);          // wszystkie funkcje Zory naraz
                        next();
                    } else if (ticks > 3000) throw new IllegalStateException("Świat się nie załadował");
                } else if (step == 6 && ticks == 80) {
                    mc.player.setYaw(mc.player.getYaw() + 40f);
                } else if (step == 6 && ticks > 120) {
                    shot(mc, "05-world-all-modules");
                    mc.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(mc.player));
                    next();
                } else if (step == 7 && ticks > 30) {
                    shot(mc, "06-inventory");
                    mc.setScreen(null);
                    mc.player.jump();
                    next();
                } else if (step == 8 && ticks > 40) {
                    shot(mc, "07-jump");
                    for (var m : Modules.ALL) m.setEnabled(false);
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
