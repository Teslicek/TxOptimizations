package com.teslicek.txoptimizations;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockStateCacheBatch {

    private static List<BlockState> pending = new ArrayList<>();

    private BlockStateCacheBatch() {
    }

    public static void defer(BlockState state) {
        pending.add(state);
    }

    public static void buildAll() {
        List<BlockState> states = pending;

        pending = new ArrayList<>();
        states.parallelStream().forEach(BlockState::initCache);
    }
}
