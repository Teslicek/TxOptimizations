package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.DestructionQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.teslicek.txoptimizations.BackgroundDestroyer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderBackgroundDestroyMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/DestructionQueue;<init>(ILcom/mojang/renderpearl/backend/vulkan/DestructionQueue$Destroyer;)V"), index = 1)
    private DestructionQueue.Destroyer<?> txoptimizations$destroyInBackground(DestructionQueue.Destroyer<?> destroyer) {
        return new BackgroundDestroyer();
    }
}
