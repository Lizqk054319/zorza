package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class PingHud extends TextHud {
    public PingHud() {
        super("ping", "Ping", "Connection latency to the server.", Category.HUD, false, 0.01, 0.25);
    }

    public ItemStack icon() { return new ItemStack(Items.ECHO_SHARD); }

    public String text(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return "0 ms";
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return (e == null ? 0 : e.getLatency()) + " ms";
    }
}
