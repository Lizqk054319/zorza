package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import pl.zorza.client.ZorzaClient;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.TextHud;

public class ComboCounterHud extends TextHud {
    public ComboCounterHud() {
        super("combo_counter", "Combo Counter", "Hits in a row without taking damage.", Category.COMBAT, false, 0.18, 0.1);
    }

    public String icon() { return "3x"; }

    public String text(MinecraftClient mc) { return "Combo|" + ZorzaClient.combo + "x"; }
}
