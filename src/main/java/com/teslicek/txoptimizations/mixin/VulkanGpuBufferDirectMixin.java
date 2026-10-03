package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.TexelViewCache;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanGpuBuffer.Direct.class)
public abstract class VulkanGpuBufferDirectMixin implements TexelViewCache {

    @Unique
    private static final int MAX_TEXEL_VIEWS = 8;

    @Unique
    private static final int TEXEL_VIEW_STRIDE = 4;

    @Shadow
    @Final
    protected VulkanDevice device;

    @Shadow
    @Final
    private long vmaAllocation;

    @Unique
    private long txoptimizations$mappedPointer;

    @Unique
    private long[] txoptimizations$texelViews;

    @Unique
    private int txoptimizations$texelViewCount;

    @WrapOperation(method = "map", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/vma/Vma;vmaMapMemory(JJLorg/lwjgl/PointerBuffer;)I"))
    private int txoptimizations$keepMapped(long allocator, long allocation, PointerBuffer pointer, Operation<Integer> original) {
        if (this.txoptimizations$mappedPointer == 0L) {
            int result = original.call(allocator, allocation, pointer);

            if (result != VK10.VK_SUCCESS)
                return result;

            this.txoptimizations$mappedPointer = pointer.get(0);
        }

        pointer.put(0, this.txoptimizations$mappedPointer);

        return VK10.VK_SUCCESS;
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void txoptimizations$releaseMappingAndViews(CallbackInfo ci) {
        if (this.txoptimizations$mappedPointer != 0L) {
            Vma.vmaUnmapMemory(this.device.vma(), this.vmaAllocation);
            this.txoptimizations$mappedPointer = 0L;
        }

        for (int i = 0; i < this.txoptimizations$texelViewCount; i ++)
            VK12.vkDestroyBufferView(this.device.vkDevice(), this.txoptimizations$texelViews[i * TEXEL_VIEW_STRIDE + 3], null);

        this.txoptimizations$texelViewCount = 0;
    }

    @Override
    public long txoptimizations$findTexelView(long offset, long range, int format) {
        long[] views = this.txoptimizations$texelViews;

        for (int i = 0; i < this.txoptimizations$texelViewCount; i ++) {
            int base = i * TEXEL_VIEW_STRIDE;

            if (views[base] == offset && views[base + 1] == range && views[base + 2] == format)
                return views[base + 3];
        }

        return 0L;
    }

    @Override
    public boolean txoptimizations$storeTexelView(long offset, long range, int format, long view) {
        if (this.txoptimizations$texelViewCount == MAX_TEXEL_VIEWS)
            return false;

        if (this.txoptimizations$texelViews == null)
            this.txoptimizations$texelViews = new long[MAX_TEXEL_VIEWS * TEXEL_VIEW_STRIDE];

        int base = this.txoptimizations$texelViewCount * TEXEL_VIEW_STRIDE;

        this.txoptimizations$texelViews[base]     = offset;
        this.txoptimizations$texelViews[base + 1] = range;
        this.txoptimizations$texelViews[base + 2] = format;
        this.txoptimizations$texelViews[base + 3] = view;
        this.txoptimizations$texelViewCount ++;

        return true;
    }
}
