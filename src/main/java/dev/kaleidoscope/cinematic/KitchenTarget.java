package dev.kaleidoscope.cinematic;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IChoppingBoard;
import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IPot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class KitchenTarget {
    public static boolean supported(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var be = level.getBlockEntity(pos);
        return (KitchenConfig.BOARD.get() && be instanceof IChoppingBoard)
            || (KitchenConfig.POT.get() && be instanceof IPot);
    }
    private KitchenTarget() {}
}
