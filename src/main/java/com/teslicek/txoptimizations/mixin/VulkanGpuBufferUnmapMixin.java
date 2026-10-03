package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer$Direct$1")
public abstract class VulkanGpuBufferUnmapMixin {

    @WrapWithCondition(method = "run", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/vma/Vma;vmaUnmapMemory(JJ)V"))
    private boolean txoptimizations$stayMapped(long allocator, long allocation) {
        return false;
    }
}
