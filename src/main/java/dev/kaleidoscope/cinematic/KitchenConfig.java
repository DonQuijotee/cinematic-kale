package dev.kaleidoscope.cinematic;

import net.neoforged.neoforge.common.ModConfigSpec;
public final class KitchenConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BOARD, POT;
    static {
        var b = new ModConfigSpec.Builder();
        BOARD = b.comment("Enable the independent chopping board scene").define("choppingBoard", true);
        POT = b.comment("Experimental pot scene; enable after testing in your pack").define("pot", false);
        SPEC = b.build();
    }
    private KitchenConfig() {}
}
