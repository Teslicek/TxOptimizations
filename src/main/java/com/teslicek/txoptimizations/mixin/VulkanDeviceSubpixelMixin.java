package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanPhysicalDevice;
import com.teslicek.txoptimizations.SubpixelPrecision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceSubpixelMixin implements SubpixelPrecision {

    @Unique
    private int txoptimizations$subpixelBits;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;close()V"))
    private void txoptimizations$readSubpixelBits(CallbackInfo ci, @Local(argsOnly = true) VulkanPhysicalDevice physicalDevice) {
        this.txoptimizations$subpixelBits = physicalDevice.vkPhysicalDeviceProperties().limits().subPixelPrecisionBits();
    }

    @Override
    public int txoptimizations$subpixelBits() {
        return this.txoptimizations$subpixelBits;
    }
}
