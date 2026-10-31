package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.ProjectionType;
import com.teslicek.txoptimizations.ParallelFeatures;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderTypeGroupParallelMixin {

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;prepare()Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;"))
    private PreparedRenderType txoptimizations$prepareLocked(RenderType renderType) {
        return ParallelFeatures.prepare(renderType);
    }

    @Redirect(method = "getOrAddDraw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getProjectionType()Lcom/mojang/blaze3d/ProjectionType;"))
    private ProjectionType txoptimizations$projectionType() {
        return ParallelFeatures.projectionType();
    }
}
