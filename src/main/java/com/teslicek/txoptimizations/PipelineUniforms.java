package com.teslicek.txoptimizations;

public interface PipelineUniforms {

    String[] txoptimizations$uniformNames();

    int[] txoptimizations$uniformSlots();

    int txoptimizations$uniformSlot(String name);
}
