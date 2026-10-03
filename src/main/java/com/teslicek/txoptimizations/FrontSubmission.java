package com.teslicek.txoptimizations;

import org.lwjgl.vulkan.VkCommandBuffer;

public interface FrontSubmission {

    void txoptimizations$executeFirst(VkCommandBuffer commandBuffer);
}
