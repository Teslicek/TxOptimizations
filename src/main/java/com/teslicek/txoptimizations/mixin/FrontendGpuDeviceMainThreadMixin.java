package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import com.teslicek.txoptimizations.RenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FrontendGpuDevice.class)
public abstract class FrontendGpuDeviceMainThreadMixin {

    @Inject(method = {"createCommandEncoder()Lcom/mojang/renderpearl/api/commands/CommandEncoder;", "createSampler(Lcom/mojang/renderpearl/api/textures/AddressMode;Lcom/mojang/renderpearl/api/textures/AddressMode;Lcom/mojang/renderpearl/api/textures/FilterMode;Lcom/mojang/renderpearl/api/textures/FilterMode;ILjava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/textures/GpuSampler;", "createTexture(Ljava/util/function/Supplier;ILcom/mojang/renderpearl/api/GpuFormat;IIII)Lcom/mojang/renderpearl/api/textures/GpuTexture;", "createTexture(Ljava/lang/String;ILcom/mojang/renderpearl/api/GpuFormat;IIII)Lcom/mojang/renderpearl/api/textures/GpuTexture;", "createTextureView(Lcom/mojang/renderpearl/api/textures/GpuTexture;)Lcom/mojang/renderpearl/api/textures/GpuTextureView;", "createTextureView(Lcom/mojang/renderpearl/api/textures/GpuTexture;II)Lcom/mojang/renderpearl/api/textures/GpuTextureView;", "createBuffer(Ljava/util/function/Supplier;IJ)Lcom/mojang/renderpearl/api/buffers/GpuBuffer;", "createBuffer(Ljava/util/function/Supplier;ILjava/nio/ByteBuffer;)Lcom/mojang/renderpearl/api/buffers/GpuBuffer;", "createTimestampQueryPool(I)Lcom/mojang/renderpearl/api/commands/GpuQueryPool;"}, at = @At("HEAD"), require = 9)
    private void txoptimizations$waitForRenderJob(CallbackInfoReturnable<?> cir) {
        RenderWorker.awaitFromMain();
    }
}
