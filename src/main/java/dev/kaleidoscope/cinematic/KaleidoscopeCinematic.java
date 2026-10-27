package dev.kaleidoscope.cinematic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(KaleidoscopeCinematic.ID)
public final class KaleidoscopeCinematic {
    public static final String ID = "kaleidoscope_cinematic";
    public KaleidoscopeCinematic(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, KitchenConfig.SPEC);
        bus.addListener(KitchenNetwork::register);
    }
}
