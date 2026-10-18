package com.teslicek.txoptimizations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

public final class SectionStateWindow {

    private static final int                             SPAN    = 18;
    private static final int                             VOLUME  = SPAN * SPAN * SPAN;
    private static final ThreadLocal<SectionStateWindow> CURRENT = ThreadLocal.withInitial(SectionStateWindow::new);

    private final BlockState[] states  = new BlockState[VOLUME];
    private final int[]        written = new int[VOLUME];
    private final Thread       owner   = Thread.currentThread();

    private int writtenCount;
    private int minX;
    private int minY;
    private int minZ;

    private SectionStateWindow() {
    }

    public static SectionStateWindow begin(SectionPos section) {
        SectionStateWindow window = CURRENT.get();

        window.clear();
        window.minX = section.minBlockX() - 1;
        window.minY = section.minBlockY() - 1;
        window.minZ = section.minBlockZ() - 1;

        return window;
    }

    public void end() {
        this.clear();
    }

    public BlockState get(SectionStateAccess region, BlockPos pos) {
        if (Thread.currentThread() != this.owner)
            return region.txoptimizations$directBlockState(pos);

        int x = pos.getX() - this.minX;
        int y = pos.getY() - this.minY;
        int z = pos.getZ() - this.minZ;

        if (x < 0 || y < 0 || z < 0 || x >= SPAN || y >= SPAN || z >= SPAN)
            return region.txoptimizations$directBlockState(pos);

        int        index = (y * SPAN + z) * SPAN + x;
        BlockState state = this.states[index];

        if (state != null)
            return state;

        state                              = region.txoptimizations$directBlockState(pos);
        this.states[index]                 = state;
        this.written[this.writtenCount ++] = index;

        return state;
    }

    private void clear() {
        for (int i = 0; i < this.writtenCount; i ++)
            this.states[this.written[i]] = null;

        this.writtenCount = 0;
    }
}
