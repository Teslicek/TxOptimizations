package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.teslicek.txoptimizations.IndirectCommandUpload;
import net.caffeinemc.mods.sodium.client.gpu.device.context.VKIndirectContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VKIndirectContext.class)
public abstract class VKIndirectContextDeviceMixin implements IndirectCommandUpload {

    @Unique
    private static final int DEVICE_USAGE = GpuBuffer.USAGE_INDIRECT_PARAMETERS | GpuBuffer.USAGE_COPY_DST;

    @Shadow
    public GpuBufferSlice.MappedView mappedView;

    @Shadow
    private int currentOffset;

    @Shadow
    private int currentSize;

    @Unique
    private GpuBuffer txoptimizations$deviceCommands;

    @ModifyArg(method = "recreateRingBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MappableRingBuffer;<init>(Ljava/util/function/Supplier;II)V"), index = 1)
    private int txoptimizations$allowCopyToDevice(int usage) {
        return usage | GpuBuffer.USAGE_COPY_SRC;
    }

    @Inject(method = "delete", at = @At("RETURN"))
    private void txoptimizations$deleteDeviceCommands(CallbackInfo ci) {
        if (this.txoptimizations$deviceCommands != null)
            this.txoptimizations$deviceCommands.close();
    }

    @Override
    public void txoptimizations$uploadCommands() {
        if (this.currentOffset == 0)
            return;

        if (this.txoptimizations$deviceCommands == null || this.txoptimizations$deviceCommands.size() < this.currentSize) {
            if (this.txoptimizations$deviceCommands != null)
                this.txoptimizations$deviceCommands.close();

            this.txoptimizations$deviceCommands = RenderSystem.getDevice().createBuffer(() -> "TxOptimizations terrain draw commands", DEVICE_USAGE, this.currentSize);
        }

        RenderSystem.getDevice().createCommandEncoder().copyToBuffer(this.mappedView.slice().slice(0, this.currentOffset), this.txoptimizations$deviceCommands.slice(0, this.currentOffset));
    }

    @Override
    public GpuBuffer txoptimizations$deviceCommands() {
        if (this.txoptimizations$deviceCommands == null)
            throw new IllegalStateException("Terrain draw commands were drawn before they were uploaded");

        return this.txoptimizations$deviceCommands;
    }
}
