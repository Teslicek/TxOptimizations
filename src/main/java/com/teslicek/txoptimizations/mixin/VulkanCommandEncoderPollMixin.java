package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import com.teslicek.txoptimizations.FrameSpin;
import com.teslicek.txoptimizations.RecyclePolling;
import java.nio.LongBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VK12;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderPollMixin {

    @Shadow
    @Final
    private VulkanDevice device;

    @Shadow
    @Final
    private long submitSemaphore;

    @Shadow
    private long currentSubmitIndex;

    @Shadow
    private long completedSubmitIndex;

    @Unique
    private final LongBuffer txoptimizations$counterValue = BufferUtils.createLongBuffer(1);

    @Inject(method = "awaitSubmitCompletion", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$pollSubmitCounter(long submitIndex, long timeout, CallbackInfoReturnable<Boolean> cir) {
        if (submitIndex <= this.completedSubmitIndex || submitIndex >= this.currentSubmitIndex)
            return;

        if (timeout != 0L) {
            if (this.txoptimizations$spinUntil(submitIndex))
                cir.setReturnValue(true);

            return;
        }

        if (RecyclePolling.isActive() && RecyclePolling.isKnownPending(submitIndex)) {
            cir.setReturnValue(false);
            return;
        }

        boolean completed = this.txoptimizations$readCounter() >= submitIndex;

        if (!completed && RecyclePolling.isActive())
            RecyclePolling.markPending(submitIndex);

        cir.setReturnValue(completed);
    }

    @Unique
    private boolean txoptimizations$spinUntil(long submitIndex) {
        if (!FrameSpin.isFastFrames())
            return false;

        long deadline = System.nanoTime() + FrameSpin.SPIN_NANOS;

        do {
            if (this.txoptimizations$readCounter() >= submitIndex)
                return true;

            Thread.onSpinWait();
        } while (System.nanoTime() < deadline);

        return false;
    }

    @Unique
    private long txoptimizations$readCounter() {
        int result = VK12.vkGetSemaphoreCounterValue(this.device.vkDevice(), this.submitSemaphore, this.txoptimizations$counterValue);
        VulkanUtils.crashIfFailure(this.device, result, "Failed to read submit semaphore counter");

        long signaled = this.txoptimizations$counterValue.get(0);

        if (signaled > this.completedSubmitIndex)
            this.completedSubmitIndex = signaled;

        return signaled;
    }
}
