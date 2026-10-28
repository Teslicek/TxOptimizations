package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.DynamicGpuDataStorage;
import org.jspecify.annotations.Nullable;

public interface LastWrittenData {

    DynamicGpuDataStorage.@Nullable DynamicGpuData txoptimizations$lastData();
}
