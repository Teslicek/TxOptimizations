package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.teslicek.txoptimizations.AnimationUploads;
import com.teslicek.txoptimizations.AtlasAnimation;
import com.teslicek.txoptimizations.RenderWorker;
import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {

    @Shadow
    private List<TextureAtlasSprite> sprites;

    @Shadow
    private List<SpriteContents.AnimationState> animatedTexturesStates;

    @Shadow
    @Final
    private Identifier location;

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private GpuTextureView[] mipViews;

    @Unique
    private AtlasAnimation txoptimizations$animation;

    @Inject(method = "upload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlas;uploadInitialContents()V"))
    private void txoptimizations$createAnimation(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        if (this.animatedTexturesStates.isEmpty())
            return;

        this.txoptimizations$animation = AtlasAnimation.create(this.location, this.sprites, this.animatedTexturesStates, this.width, this.height, this.mipViews.length);
    }

    @Inject(method = "clearTextureData", at = @At("HEAD"))
    private void txoptimizations$closeAnimation(CallbackInfo ci) {
        if (this.txoptimizations$animation == null)
            return;

        RenderWorker.awaitFromMain();
        AnimationUploads.drop(this.txoptimizations$animation);
        this.txoptimizations$animation.close();
        this.txoptimizations$animation = null;
    }

    @Overwrite
    private void uploadAnimationFrames() {
        if (this.animatedTexturesStates.isEmpty())
            return;

        if (this.txoptimizations$animation == null)
            throw new IllegalStateException("Atlas " + this.location + " drew its animated sprites before uploading them");

        this.txoptimizations$animation.prepare(this.mipViews);
    }

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
