package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanBackend.class)
public abstract class VulkanBackendCheckpointMixin {

    @WrapOperation(method = "createDevice", at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"))
    private boolean txoptimizations$skipCheckpointExtensions(Set<Object> featureSets, Object featureSet, Operation<Boolean> original) {
        if (featureSet == VulkanFeatureSets.AMD_BUFFER_MARKER_FEATURESET || featureSet == VulkanFeatureSets.NV_DIAGNOSTIC_CHECKPOINT_FEATURESET)
            return false;

        return original.call(featureSets, featureSet);
    }
}
