package com.teslicek.txoptimizations.mixin.gpu;

import com.mojang.renderpearl.backend.vulkan.VulkanQueryPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VulkanQueryPool.class)
public interface VulkanQueryPoolAccessor {

    @Invoker("vkQueryPool")
    long txoptimizations$vkQueryPool();
}
