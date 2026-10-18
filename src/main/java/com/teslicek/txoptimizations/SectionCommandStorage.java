package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

public interface SectionCommandStorage {

    GpuBufferSlice[] txoptimizations$writeSectionCommands(SectionDrawList draws);
}
