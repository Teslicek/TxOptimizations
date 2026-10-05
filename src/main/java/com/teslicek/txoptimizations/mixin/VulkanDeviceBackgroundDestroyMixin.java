package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.teslicek.txoptimizations.BackgroundDestroyer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceBackgroundDestroyMixin {

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/vma/Vma;vmaDestroyAllocator(J)V"))
    private void txoptimizations$finishBackgroundDestroys(CallbackInfo ci) {
        BackgroundDestroyer.drain();
    }
}
