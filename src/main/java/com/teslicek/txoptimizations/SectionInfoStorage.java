package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

public interface SectionInfoStorage {

    GpuBufferSlice txoptimizations$writeSectionInfos(SectionDrawList draws);
}
