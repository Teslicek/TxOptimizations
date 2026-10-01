package com.teslicek.txoptimizations.mixin.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.bake.Baking;
import com.teslicek.txoptimizations.bake.MeshedRenderState;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerRenderer.class)
public abstract class BannerRendererBakeMixin {

    @Shadow
    public SpriteGetter sprites;

    @Shadow
    protected abstract BannerFlagModel flagModel(BannerBlock.AttachmentType type);

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BannerBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"))
    private void txoptimizations$markMeshed(BannerBlockEntity banner, BannerRenderState state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        ((MeshedRenderState) state).txoptimizations$setMeshed(Baking.isBaked(banner));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BannerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$submitFlagOnly(BannerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!((MeshedRenderState) state).txoptimizations$isMeshed())
            return;

        ci.cancel();

        BannerFlagModel flag   = this.flagModel(state.attachmentType);
        SpriteId        sprite = Sheets.BANNER_BASE;

        poseStack.pushPose();
        poseStack.mulPose(state.transformation);
        collector.submitModel(flag, state.phase, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, this.sprites, 0);

        if (state.breakProgress != null)
            collector.order(state.patterns.layers().size() + 2).submitCrumblingOverlay(flag, state.phase, poseStack, sprite.renderType(flag.renderType()), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);

        BannerRenderer.submitPatterns(this.sprites, poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, flag, state.phase, true, state.baseColor, state.patterns);
        poseStack.popPose();
    }
}
