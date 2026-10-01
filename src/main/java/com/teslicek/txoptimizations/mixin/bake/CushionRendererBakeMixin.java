package com.teslicek.txoptimizations.mixin.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teslicek.txoptimizations.bake.Bakeable;
import com.teslicek.txoptimizations.bake.MeshedRenderState;
import com.teslicek.txoptimizations.bake.RenderMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CushionRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.decoration.Cushion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CushionRenderer.class)
public abstract class CushionRendererBakeMixin extends EntityRenderer<Cushion, CushionRenderState> {

    protected CushionRendererBakeMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/Cushion;Lnet/minecraft/client/renderer/entity/state/CushionRenderState;F)V", at = @At("TAIL"))
    private void txoptimizations$markMeshed(Cushion cushion, CushionRenderState state, float partialTick, CallbackInfo ci) {
        ((MeshedRenderState) state).txoptimizations$setMeshed(((Bakeable) cushion).txoptimizations$getRenderMode() == RenderMode.TERRAIN);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/CushionRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$submitNameOnly(CushionRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!((MeshedRenderState) state).txoptimizations$isMeshed())
            return;

        super.submit(state, poseStack, collector, camera);
        ci.cancel();
    }
}
