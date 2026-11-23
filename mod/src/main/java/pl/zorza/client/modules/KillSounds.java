package pl.zorza.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import pl.zorza.client.CustomSounds;
import pl.zorza.client.module.Category;
import pl.zorza.client.module.Module;
import pl.zorza.client.module.Setting;

import java.util.Map;

public class KillSounds extends Module implements SoundModule {
    private static final Map<String, SoundEvent> BUILTIN = CustomSounds.map(
            "Level up", SoundEvents.ENTITY_PLAYER_LEVELUP,
            "Bell", SoundEvents.BLOCK_BELL_USE,
            "Pling", SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
            "Blaze", SoundEvents.ENTITY_BLAZE_DEATH);
    private final Setting.Choice sound = add(new Setting.Choice("sound", "Sound", "Level up", () -> CustomSounds.options(BUILTIN)));
    private final Setting.Bool playersOnly = add(new Setting.Bool("players_only", "Only players", false));
    private final Setting.Num volume = add(new Setting.Num("volume", "Volume", 0.8, 0.1, 1.0, 0.05));
    private final Setting.Num pitch = add(new Setting.Num("pitch", "Pitch", 1.0, 0.5, 2.0, 0.05));
    private Entity target;
    private long hitAt;

    public KillSounds() {
        super("kill_sounds", "Kill Sounds", "Plays a sound when something you hit dies. Own sounds: sounds folder.", Category.SOUND, false);
    }

    public String icon() { return "m_sound"; }

    public void preview() { CustomSounds.play(sound.value, BUILTIN, (float) volume.value, (float) pitch.value); }

    public void onHit(Entity e) {
        if (!(e instanceof LivingEntity)) return;
        target = e;
        hitAt = System.currentTimeMillis();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (target == null) return;
        if (System.currentTimeMillis() - hitAt > 3000) { target = null; return; }
        if (target instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0f)) {
            if (!playersOnly.value || le.isPlayer()) preview();
            target = null;
        }
    }
}
