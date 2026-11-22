package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class ServerAddressHud extends TextHud {
    public ServerAddressHud() {
        super("server_address", "Server Address", "The server you are playing on.", Category.HUD, false, 0.01, 0.35);
    }

    public String icon() { return "m_server"; }

    public String text(MinecraftClient mc) {
        if (mc.isInSingleplayer()) return "Singleplayer";
        var s = mc.getCurrentServerEntry();
        return s == null ? "Menu" : s.address;
    }
}
