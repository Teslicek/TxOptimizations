package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.DestructionQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanUtils;
import com.teslicek.txoptimizations.BackgroundDestroyer;
import com.teslicek.txoptimizations.DynamicStateCache;
import com.teslicek.txoptimizations.FramePath;
import com.teslicek.txoptimizations.FrameSpin;
import com.teslicek.txoptimizations.InFlightVertexCopy;
import com.teslicek.txoptimizations.PresentThread;
import com.teslicek.txoptimizations.RecyclePolling;
import com.teslicek.txoptimizations.RenderWorker;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.LongBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderMixin {

    @Inject(method = "allocateAndBeginTransientCommandBuffer", at = @At("HEAD"), require = 1)
    private void txoptimizations$requirePoolOwner(CallbackInfoReturnable<?> cir) {
        if (RenderWorker.isBusy() && !RenderWorker.isCurrent())
            throw new IllegalStateException("Thread " + Thread.currentThread().getName() + " allocated a command buffer while the render job owns the command pool");
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/DestructionQueue;<init>(ILcom/mojang/renderpearl/backend/vulkan/DestructionQueue$Destroyer;)V"), index = 1)
    private DestructionQueue.Destroyer<?> txoptimizations$destroyInBackground(DestructionQueue.Destroyer<?> destroyer) {
        return new BackgroundDestroyer();
    }

    @Inject(method = "allocateAndBeginTransientCommandBuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VK12;vkBeginCommandBuffer(Lorg/lwjgl/vulkan/VkCommandBuffer;Lorg/lwjgl/vulkan/VkCommandBufferBeginInfo;)I"))
    private void txoptimizations$forgetDynamicState(CallbackInfoReturnable<?> cir) {
        DynamicStateCache.reset();
    }

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
    private static final FunctionDescriptor COUNTER_CALL = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG);

    @Unique
    private final LongBuffer txoptimizations$counterValue = BufferUtils.createLongBuffer(1);

    @Unique
    private MethodHandle txoptimizations$counterCall;

    @Unique
    private long txoptimizations$deviceAddress;

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
        MethodHandle call = this.txoptimizations$counterCall;

        if (call == null)
            call = this.txoptimizations$bindCounterCall();

        int result;

        try {
            result = (int) call.invokeExact(this.txoptimizations$deviceAddress, this.submitSemaphore, MemoryUtil.memAddress(this.txoptimizations$counterValue));
        } catch (Throwable thrown) {
            throw new IllegalStateException("Reading the submit semaphore counter failed", thrown);
        }

        VulkanUtils.crashIfFailure(this.device, result, "Failed to read submit semaphore counter");

        long signaled = this.txoptimizations$counterValue.get(0);

        if (signaled > this.completedSubmitIndex)
            this.completedSubmitIndex = signaled;

        return signaled;
    }

    @Unique
    private MethodHandle txoptimizations$bindCounterCall() {
        VkDevice vkDevice = this.device.vkDevice();
        long     function = vkDevice.getCapabilities().vkGetSemaphoreCounterValue;

        if (function == 0L)
            throw new IllegalStateException("The Vulkan device has no vkGetSemaphoreCounterValue");

        MethodHandle call = Linker.nativeLinker().downcallHandle(MemorySegment.ofAddress(function), COUNTER_CALL, Linker.Option.critical(false));

        this.txoptimizations$deviceAddress = vkDevice.address();
        this.txoptimizations$counterCall   = call;

        return call;
    }

    @Shadow
    private boolean awaitSubmitCompletion(long submitIndex, long timeoutNS) {
        throw new AssertionError();
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;awaitSubmitCompletion(JJ)Z"))
    private boolean txoptimizations$measureGpuWait(VulkanCommandEncoder encoder, long submitIndex, long timeout) {
        long    start  = System.nanoTime();
        boolean result = ((VulkanCommandEncoderMixin) (Object) encoder).awaitSubmitCompletion(submitIndex, timeout);

        FramePath.recordGpuWait(System.nanoTime() - start);

        return result;
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanQueue$Submission;close()V"))
    private void txoptimizations$submitOnPresentThread(VulkanQueue.Submission submission) {
        if (!PresentThread.isArmed()) {
            submission.close();

            return;
        }

        PresentThread.handOff(submission);
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void txoptimizations$finishVertexCopies(CallbackInfo ci) {
        InFlightVertexCopy.finish();
    }
}
