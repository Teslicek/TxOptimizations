package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.teslicek.txoptimizations.DepthBounds;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceDepthBoundsMixin {

    @Shadow
    @Final
    private FeatureSet enabledFeatures;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$noteDepthBounds(CallbackInfo ci) {
        DepthBounds.deviceCreated(this.enabledFeatures);
    }
}
