package com.teslicek.txoptimizations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface SectionStateAccess {

    BlockState txoptimizations$directBlockState(BlockPos pos);

    void txoptimizations$windowStates(SectionStateWindow window);
}
