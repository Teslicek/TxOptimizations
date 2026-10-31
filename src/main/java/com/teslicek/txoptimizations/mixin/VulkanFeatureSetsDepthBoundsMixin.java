package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.teslicek.txoptimizations.DepthBounds;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanFeatureSets.class)
public abstract class VulkanFeatureSetsDepthBoundsMixin {

    @Inject(method = "optionalFeatureSets", at = @At("RETURN"))
    private static void txoptimizations$requestDepthBounds(CallbackInfoReturnable<Set<FeatureSet>> cir) {
        cir.getReturnValue().add(DepthBounds.FEATURES);
    }
}
