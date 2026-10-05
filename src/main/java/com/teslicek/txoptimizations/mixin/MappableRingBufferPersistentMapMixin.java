package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MappableRingBuffer.class)
public abstract class MappableRingBufferPersistentMapMixin implements PersistentRingBuffer {

    @Shadow
    @Final
    private GpuBuffer[] buffers;

    @Shadow
    private int current;

    @Unique
    private GpuBufferSlice.MappedView[] txoptimizations$views;

    @Shadow
    public abstract GpuBuffer currentBuffer();

    @Override
    public ByteBuffer txoptimizations$mapCurrent() {
        GpuBuffer buffer = this.currentBuffer();

        if (this.txoptimizations$views == null)
            this.txoptimizations$views = new GpuBufferSlice.MappedView[this.buffers.length];

        GpuBufferSlice.MappedView view = this.txoptimizations$views[this.current];

        if (view == null) {
            view                                     = buffer.map(false, true);
            this.txoptimizations$views[this.current] = view;
        }

        return view.data();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$unmap(CallbackInfo ci) {
        if (this.txoptimizations$views == null)
            return;

        for (GpuBufferSlice.MappedView view : this.txoptimizations$views) {
            if (view != null)
                view.close();
        }

        this.txoptimizations$views = null;
    }
}
