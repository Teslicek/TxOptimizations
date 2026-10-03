package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.teslicek.txoptimizations.IndirectCommandSlots;
import com.teslicek.txoptimizations.IndirectSlotRecord;
import java.util.IdentityHashMap;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.gpu.device.context.VKIndirectContext;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VKIndirectContext.class, remap = false)
public abstract class VKIndirectContextSlotMixin implements IndirectCommandSlots {

    @Shadow
    private MappableRingBuffer ringBuffer;

    @Unique
    private final Map<GpuBuffer, IndirectSlotRecord> txoptimizations$records = new IdentityHashMap<>();

    @Unique
    private IndirectSlotRecord txoptimizations$current;

    @Unique
    private int txoptimizations$position;

    @Unique
    private boolean txoptimizations$diverged;

    @Inject(method = "recreateRingBuffer", at = @At("TAIL"))
    private void txoptimizations$moveRecord(int size, CallbackInfo ci) {
        IndirectSlotRecord record = this.txoptimizations$current == null ? new IndirectSlotRecord() : this.txoptimizations$current;

        this.txoptimizations$records.clear();
        this.txoptimizations$records.put(this.ringBuffer.currentBuffer(), record);
        this.txoptimizations$current = record;
    }

    @Inject(method = "rotate", at = @At("HEAD"))
    private void txoptimizations$closeRecord(CallbackInfo ci) {
        this.txoptimizations$current.setCount(this.txoptimizations$position);
    }

    @Inject(method = "rotate", at = @At("TAIL"))
    private void txoptimizations$openRecord(CallbackInfo ci) {
        this.txoptimizations$current  = this.txoptimizations$records.computeIfAbsent(this.ringBuffer.currentBuffer(), buffer -> new IndirectSlotRecord());
        this.txoptimizations$position = 0;
        this.txoptimizations$diverged = false;
    }

    @Override
    public boolean txoptimizations$isWritten(Object batch, int version) {
        int index = this.txoptimizations$position ++;

        if (!this.txoptimizations$diverged && this.txoptimizations$current.matches(index, batch, version))
            return true;

        this.txoptimizations$diverged = true;
        this.txoptimizations$current.store(index, batch, version);

        return false;
    }
}
