package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import com.teslicek.txoptimizations.FrontSubmission;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VulkanQueue.Submission.class)
public abstract class VulkanSubmissionFrontMixin implements FrontSubmission {

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
