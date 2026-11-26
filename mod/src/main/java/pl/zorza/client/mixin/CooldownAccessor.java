package pl.zorza.client.mixin;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemCooldownManager.class)
public interface CooldownAccessor {
    @Accessor("entries")
    Map<?, ?> zorza$entries();

    @Accessor("tick")
    int zorza$tick();
}
