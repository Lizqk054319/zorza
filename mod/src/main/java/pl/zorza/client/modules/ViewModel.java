package pl.zorza.client.modules;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

/** ViewModel: pozycja, obrót i rozmiar przedmiotu w ręce (pierwsza osoba). */
public class ViewModel extends Module {
    private final Setting.Num[] main = hand("main", "Main hand");
    public final Setting.Bool mirror = add(new Setting.Bool("mirror", "Off hand = mirrored main hand", true));
    private final Setting.Num[] off = hand("off", "Off hand");

    public ViewModel() {
        super("viewmodel", "ViewModel", "Move, rotate and resize the item in your hand.", Category.UTILITY, false);
    }

    public String icon() { return "m_items"; }

    private Setting.Num[] hand(String id, String name) {
        return new Setting.Num[]{
                add(new Setting.Num(id + "_rx", name + " · rotation X", 0, -180, 180, 1)),
                add(new Setting.Num(id + "_ry", name + " · rotation Y", 0, -180, 180, 1)),
                add(new Setting.Num(id + "_rz", name + " · rotation Z", 0, -180, 180, 1)),
                add(new Setting.Num(id + "_tx", name + " · position X", 0, -1, 1, 0.01)),
                add(new Setting.Num(id + "_ty", name + " · position Y", 0, -1, 1, 0.01)),
                add(new Setting.Num(id + "_tz", name + " · position Z", 0, -1, 1, 0.01)),
                add(new Setting.Num(id + "_sx", name + " · scale X", 1, 0.1, 2, 0.01)),
                add(new Setting.Num(id + "_sy", name + " · scale Y", 1, 0.1, 2, 0.01)),
                add(new Setting.Num(id + "_sz", name + " · scale Z", 1, 0.1, 2, 0.01)),
        };
    }

    /** right = ręka po prawej stronie ekranu; mainHand = czy to ręka główna. */
    public void apply(MatrixStack m, boolean mainHand, boolean right) {
        Setting.Num[] v = mainHand || mirror.value ? main : off;
        float side = right ? 1f : -1f;
        if (!mainHand && !mirror.value) side = right ? 1f : -1f;
        m.translate(v[3].value * side, v[4].value, v[5].value);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) v[1].value * side));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) v[0].value));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) v[2].value * side));
        m.scale((float) v[6].value, (float) v[7].value, (float) v[8].value);
    }
}
