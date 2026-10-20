package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.InFlightVertexCopy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderVertexCopyMixin {

    @Inject(method = "submit", at = @At("HEAD"))
    private void txoptimizations$finishVertexCopies(CallbackInfo ci) {
        InFlightVertexCopy.finish();
    }
}
