package pl.zorza.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import pl.zorza.client.Config;
import pl.zorza.client.Modules;
import pl.zorza.client.gui.HudEditorScreen;
import pl.zorza.client.gui.ModMenuScreen;
import pl.zorza.client.module.Module;

public class ZorzaScreenshots implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder().create()) {
            sp.getClientWorld().waitForChunksRender();
            context.runOnClient(client -> {
                for (Module m : Modules.ALL) {
                    if (m.id.equals("item_counter") || m.id.equals("reach_display") || m.id.equals("keystrokes")) m.enabled = true;
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("01-hud");
            context.runOnClient(client -> client.setScreen(new HudEditorScreen(true)));
            context.waitTicks(30);
            context.takeScreenshot("02-right-shift");
            context.runOnClient(client -> client.setScreen(new ModMenuScreen(null)));
            context.waitTicks(30);
            context.takeScreenshot("03-mod-menu");
            context.runOnClient(client -> client.setScreen(new ModMenuScreen(Modules.ALL.get(3))));
            context.waitTicks(30);
            context.takeScreenshot("04-settings");
            context.runOnClient(client -> {
                Config.compact = true;
                client.setScreen(new ModMenuScreen(null));
            });
            context.waitTicks(30);
            context.takeScreenshot("05-compact");
            context.runOnClient(client -> {
                Config.compact = false;
                client.setScreen(null);
                org.lwjgl.glfw.GLFW.glfwSetWindowSize(client.getWindow().getHandle(), 1600, 900);
            });
            context.waitTicks(40);
            context.runOnClient(client -> client.setScreen(new HudEditorScreen(true)));
            context.waitTicks(30);
            context.takeScreenshot("06-right-shift-big");
            context.runOnClient(client -> client.setScreen(new ModMenuScreen(null)));
            context.waitTicks(30);
            context.takeScreenshot("07-mod-menu-big");
            context.runOnClient(client -> client.setScreen(null));
        }
    }
}
