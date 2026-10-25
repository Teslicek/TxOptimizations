package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.Objects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GpuBufferSlice.class, priority = 999)
public abstract class GpuBufferSliceEqualityMixin {

    @Unique
    private static final int PRIME = 31;

    @Shadow
    @Final
    private GpuBuffer buffer;

    @Shadow
    @Final
    private long offset;

    @Shadow
    @Final
    private long length;

    @Overwrite
    public final boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof GpuBufferSlice that))
            return false;

        return this.offset == that.offset() && this.length == that.length() && Objects.equals(this.buffer, that.buffer());
    }

    @Overwrite
    public final int hashCode() {
        int hash = Objects.hashCode(this.buffer);

        hash = hash * PRIME + Long.hashCode(this.offset);

        return hash * PRIME + Long.hashCode(this.length);
    }
}
