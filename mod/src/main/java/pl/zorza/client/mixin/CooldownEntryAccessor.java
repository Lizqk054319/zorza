package pl.zorza.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface CooldownEntryAccessor {
    @Accessor("startTick")
    int zorza$start();

    @Accessor("endTick")
    int zorza$end();
}
