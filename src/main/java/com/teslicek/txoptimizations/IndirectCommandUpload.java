package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.buffers.GpuBuffer;

public interface IndirectCommandUpload {

    void txoptimizations$uploadCommands();

    GpuBuffer txoptimizations$deviceCommands();
}
