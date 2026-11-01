package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import com.teslicek.txoptimizations.RenderThreadStack;
import com.teslicek.txoptimizations.RenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendGpuDevice.class)
public abstract class FrontendGpuDeviceMainThreadMixin {

    @Inject(method = {"createCommandEncoder", "createSampler", "createTexture", "createTextureView", "createBuffer", "createTimestampQueryPool"}, at = @At("HEAD"), require = 9)
    private void txoptimizations$waitForRenderJob(CallbackInfo ci) {
        if (RenderThreadStack.isMain())
            RenderWorker.awaitFrame();
    }
}
