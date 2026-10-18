package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

public interface SectionDrawUniforms {

    GpuBufferSlice[] txoptimizations$writeSectionCommands(SectionDrawList draws);

    GpuBufferSlice txoptimizations$writeSectionInfos(SectionDrawList draws);
}
