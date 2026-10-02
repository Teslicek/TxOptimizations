package com.teslicek.txoptimizations;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkMemoryBarrier2;

public final class PreciseBarriers {

    private static final long PASS_SOURCE_STAGES   = VK13.VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT;
    private static final long PASS_SOURCE_ACCESS   = VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT | VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
    private static final long PASS_TARGET_STAGES   = VK13.VK_PIPELINE_STAGE_2_VERTEX_SHADER_BIT | VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT | VK13.VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT | VK13.VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT;
    private static final long PASS_TARGET_ACCESS   = VK13.VK_ACCESS_2_SHADER_READ_BIT | VK13.VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT | VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK13.VK_ACCESS_2_TRANSFER_READ_BIT | VK13.VK_ACCESS_2_TRANSFER_WRITE_BIT;
    private static final long UPLOAD_SOURCE_STAGES = VK13.VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT;
    private static final long UPLOAD_SOURCE_ACCESS = VK13.VK_ACCESS_2_TRANSFER_WRITE_BIT;
    private static final long UPLOAD_TARGET_STAGES = VK13.VK_PIPELINE_STAGE_2_DRAW_INDIRECT_BIT | VK13.VK_PIPELINE_STAGE_2_VERTEX_INPUT_BIT | VK13.VK_PIPELINE_STAGE_2_VERTEX_SHADER_BIT | VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT | VK13.VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT;
    private static final long UPLOAD_TARGET_ACCESS = VK13.VK_ACCESS_2_INDIRECT_COMMAND_READ_BIT | VK13.VK_ACCESS_2_INDEX_READ_BIT | VK13.VK_ACCESS_2_VERTEX_ATTRIBUTE_READ_BIT | VK13.VK_ACCESS_2_UNIFORM_READ_BIT | VK13.VK_ACCESS_2_SHADER_READ_BIT | VK13.VK_ACCESS_2_TRANSFER_READ_BIT | VK13.VK_ACCESS_2_TRANSFER_WRITE_BIT;

    private PreciseBarriers() {
    }

    public static void afterRenderPass(VkCommandBuffer commandBuffer) {
        record(commandBuffer, PASS_SOURCE_STAGES, PASS_SOURCE_ACCESS, PASS_TARGET_STAGES, PASS_TARGET_ACCESS);
    }

    public static void afterUpload(VkCommandBuffer commandBuffer) {
        record(commandBuffer, UPLOAD_SOURCE_STAGES, UPLOAD_SOURCE_ACCESS, UPLOAD_TARGET_STAGES, UPLOAD_TARGET_ACCESS);
    }

    private static void record(VkCommandBuffer commandBuffer, long sourceStages, long sourceAccess, long targetStages, long targetAccess) {
        if (commandBuffer == null)
            throw new IllegalStateException("No command buffer is recording");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryBarrier2.Buffer barrier = VkMemoryBarrier2.calloc(1, stack).sType$Default();

            barrier.srcStageMask(sourceStages);
            barrier.srcAccessMask(sourceAccess);
            barrier.dstStageMask(targetStages);
            barrier.dstAccessMask(targetAccess);
            KHRSynchronization2.vkCmdPipelineBarrier2KHR(commandBuffer, VkDependencyInfo.calloc(stack).sType$Default().pMemoryBarriers(barrier));
        }
    }
}
