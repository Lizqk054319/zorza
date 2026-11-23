package pl.zorza.client.modules;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import pl.zorza.client.CustomSounds;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.Map;

public class HitSounds extends Module implements SoundModule {
    private static final Map<String, SoundEvent> BUILTIN = CustomSounds.map(
            "Orb", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            "Arrow hit", SoundEvents.ENTITY_ARROW_HIT_PLAYER,
            "Pling", SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
            "Anvil", SoundEvents.BLOCK_ANVIL_LAND);
    private final Setting.Choice sound = add(new Setting.Choice("sound", "Sound", "Orb", () -> CustomSounds.options(BUILTIN)));
    private final Setting.Num volume = add(new Setting.Num("volume", "Volume", 0.8, 0.1, 1.0, 0.05));
    private final Setting.Num pitch = add(new Setting.Num("pitch", "Pitch", 1.0, 0.5, 2.0, 0.05));

    public HitSounds() {
        super("hit_sounds", "Hit Sounds", "Plays a sound when you hit something. Own sounds: sounds folder.", Category.SOUND, false);
    }

    public String icon() { return "m_sound"; }

    public void preview() { CustomSounds.play(sound.value, BUILTIN, (float) volume.value, (float) pitch.value); }

    public void onHit() {
        if (enabled) preview();
    }
}
