package dev.kaleidoscope.cinematic;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = KaleidoscopeCinematic.ID)
public final class VanillaFallback {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sneaking(PlayerInteractEvent.RightClickBlock event) {
        // Force the original block handler through vanilla's sneak suppression, but respect explicit denials.
        if (event.getEntity().isShiftKeyDown() && KitchenTarget.supported(event.getLevel(),event.getPos())
                && event.getUseBlock() == TriState.DEFAULT) {
            event.setUseBlock(TriState.TRUE);
        }
    }
    private VanillaFallback() {}
}
