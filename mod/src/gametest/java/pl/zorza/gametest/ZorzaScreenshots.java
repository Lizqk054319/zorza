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
            context.runOnClient(client -> {
                Modules.FREELOOK.active = true;
                Modules.FREELOOK.yaw = client.player.getYaw() + 150;
                Modules.FREELOOK.pitch = 25;
                client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
                Modules.TIME_CHANGER.enabled = true;
                Modules.TIME_CHANGER.time.value = "Sunset";
                client.player.getInventory().setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.COOKED_BEEF, 25));
            });
            context.waitTicks(20);
            context.takeScreenshot("15-freelook");
            context.runOnClient(client -> {
                Modules.FREELOOK.active = false;
                client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
                client.player.getInventory().selectedSlot = 1;
                Modules.VIEW_MODEL.enabled = true;
                for (var st : Modules.VIEW_MODEL.settings) {
                    if (st.id.equals("main_ry") && st instanceof pl.zorza.client.module.Setting.Num n) n.value = -25;
                    if (st.id.equals("main_tx") && st instanceof pl.zorza.client.module.Setting.Num n) n.value = -0.15;
                    if (st.id.equals("main_sx") && st instanceof pl.zorza.client.module.Setting.Num n) n.value = 1.4;
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("16-viewmodel-hotbar");
            context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player)));
            context.waitTicks(20);
            context.takeScreenshot("17-inventory");
            context.runOnClient(client -> {
                client.setScreen(null);
                Modules.VIEW_MODEL.enabled = false;
                Modules.TIME_CHANGER.enabled = false;
                client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
            });
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
            context.runOnClient(client -> client.setScreen(new pl.zorza.client.gui.ZoraModsScreen(null)));
            context.waitTicks(30);
            context.takeScreenshot("10-zora-mods");
            context.runOnClient(client -> client.setScreen(new ModMenuScreen(Modules.KILL_SOUNDS)));
            context.waitTicks(30);
            context.takeScreenshot("08-kill-sounds");
            context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.GameMenuScreen(true)));
            context.waitTicks(30);
            context.takeScreenshot("12-pause");
            context.runOnClient(client -> client.setScreen(null));
        }
        // okno "Połączyć z serwerem?" (dane jak z launchera)
        context.runOnClient(client -> {
            java.util.List<net.minecraft.text.Text> motd = java.util.List.of(
                    net.minecraft.text.Text.literal("ANARCHIA.GG ").formatted(net.minecraft.util.Formatting.WHITE, net.minecraft.util.Formatting.BOLD)
                            .append(net.minecraft.text.Text.literal("» WBIJAJ NA DC.ANARCHIA.GG").formatted(net.minecraft.util.Formatting.YELLOW)),
                    net.minecraft.text.Text.literal("NAPÓJ ANARCHIA JUŻ DOSTĘPNY W ŻABKACH").formatted(net.minecraft.util.Formatting.GREEN));
            client.setScreen(new pl.zorza.client.gui.AutoJoinScreen(new net.minecraft.client.gui.screen.TitleScreen(),
                    new pl.zorza.client.AutoJoin.Data("anarchia.gg", "anarchia.gg", motd, 17316, 17317, null)));
        });
        context.waitTicks(40);
        context.takeScreenshot("09-autojoin");
        context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.TitleScreen()));
        context.waitTicks(40);
        context.takeScreenshot("11-title");
        context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.option.VideoOptionsScreen(
                new net.minecraft.client.gui.screen.TitleScreen(), client, client.options)));
        context.waitTicks(30);
        context.takeScreenshot("13-video-options");
        context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(new net.minecraft.client.gui.screen.TitleScreen())));
        context.waitTicks(30);
        context.takeScreenshot("14-multiplayer");
        context.runOnClient(client -> client.setScreen(new net.minecraft.client.gui.screen.TitleScreen()));
        context.waitTicks(10);
    }
}
