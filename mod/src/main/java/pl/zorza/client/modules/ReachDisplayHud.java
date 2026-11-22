package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

/** Pokazuje odległość ostatniego uderzenia (tylko wyświetlanie, nic nie zmienia). */
public class ReachDisplayHud extends TextHud {
    public ReachDisplayHud() {
        super("reach_display", "Reach Display", "Distance of your last hit.", Category.COMBAT, false, 0.18, 0.02);
    }

    public String icon() { return "m_reach"; }

    public String text(MinecraftClient mc) {
        boolean fresh = ZorzaClient.lastReach >= 0 && System.currentTimeMillis() - ZorzaClient.lastReachTime < 3000;
        return "Reach|" + (fresh ? String.format("%.2f", ZorzaClient.lastReach) : "0.00") + "|blocks";
    }
}
