package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.teslicek.txoptimizations.FrontSubmission;
import com.teslicek.txoptimizations.PresentThread;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanQueue.Submission.class)
public abstract class VulkanQueueSubmissionMixin implements FrontSubmission {

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$drainPresentThread(CallbackInfo ci) {
        PresentThread.drain();
    }

    @Shadow
    private boolean closed;

    @Shadow
    @Final
    private ReferenceArrayList<VulkanQueue.Submission.SubmitStage> stages;

    @Override
    public void txoptimizations$executeFirst(VkCommandBuffer commandBuffer) {
        if (this.closed)
            throw new IllegalStateException("Attempt to use closed Submission");

        VulkanQueue.Submission.SubmitStage first = this.stages.get(0);

        if (first.waits().isEmpty()) {
            first.commandBuffers().add(0, commandBuffer);

            return;
        }

        VulkanQueue.Submission.SubmitStage stage = new VulkanQueue.Submission.SubmitStage();

        stage.commandBuffers().add(commandBuffer);
        this.stages.add(0, stage);
    }
}
