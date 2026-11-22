package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

public class HitSounds extends Module {
    private final Setting.Num sound = add(new Setting.Num("sound", "Sound (1-4)", 1, 1, 4, 1));
    private final Setting.Num volume = add(new Setting.Num("volume", "Volume", 0.8, 0.1, 1.0, 0.05));
    private final Setting.Num pitch = add(new Setting.Num("pitch", "Pitch", 1.0, 0.5, 2.0, 0.05));

    public HitSounds() {
        super("hit_sounds", "Hit Sounds", "Plays a sound when you hit something.", Category.SOUND, false);
    }

    public String icon() { return "m_sound"; }

    private SoundEvent event() {
        return switch ((int) sound.value) {
            case 2 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case 3 -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case 4 -> SoundEvents.BLOCK_ANVIL_LAND;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
    }

    public void onHit() {
        if (!enabled) return;
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(event(), (float) pitch.value, (float) volume.value));
    }
}
