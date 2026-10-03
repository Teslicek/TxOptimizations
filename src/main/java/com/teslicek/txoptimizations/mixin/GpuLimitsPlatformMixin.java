package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.gpu.GPULimits;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GPULimits.class, remap = false)
public abstract class GpuLimitsPlatformMixin {

    @Unique
    private static final int SUB_TEXEL_PRECISION_BITS = Util.getPlatform() == Util.OS.OSX ? 4 : 8;

    @Overwrite
    public static int getSubTexelPrecisionBits() {
        return SUB_TEXEL_PRECISION_BITS;
    }
}
