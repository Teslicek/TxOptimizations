package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.textures.GpuSampler;

public record ClampedSampler(SamplerCache cache, GpuSampler sampler) {
}
