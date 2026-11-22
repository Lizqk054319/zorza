package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class PingHud extends TextHud {
    public PingHud() {
        super("ping", "Ping", "Connection latency to the server.", Category.HUD, false, 0.01, 0.44);
    }

    public String icon() { return "m_ping"; }

    public String text(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return "0 ms";
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return "Ping|" + (e == null ? 0 : e.getLatency()) + "|ms";
    }
}
