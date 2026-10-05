package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasScratchViewMixin {

    @Unique
    private GpuTexture txoptimizations$scratchTexture;

    @Unique
    private GpuTextureView txoptimizations$scratchView;

    @WrapOperation(method = "uploadInitialContents", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuDevice;createTextureView(Lcom/mojang/renderpearl/api/textures/GpuTexture;)Lcom/mojang/renderpearl/api/textures/GpuTextureView;"))
    private GpuTextureView txoptimizations$shareScratchView(GpuDevice device, GpuTexture scratchTexture, Operation<GpuTextureView> original) {
        if (scratchTexture == this.txoptimizations$scratchTexture)
            return this.txoptimizations$scratchView;

        GpuTextureView view = original.call(device, scratchTexture);

        this.txoptimizations$scratchTexture = scratchTexture;
        this.txoptimizations$scratchView    = view;

        return view;
    }

    @Inject(method = "uploadInitialContents", at = @At("TAIL"))
    private void txoptimizations$forgetScratchView(CallbackInfo ci) {
        this.txoptimizations$scratchTexture = null;
        this.txoptimizations$scratchView    = null;
    }
}
