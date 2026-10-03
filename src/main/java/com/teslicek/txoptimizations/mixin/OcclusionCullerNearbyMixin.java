package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCullerNearbyMixin {

    @Shadow
    private OcclusionCuller.GraphOcclusionVisitor visitorWide;

    @Shadow
    private OcclusionCuller.GraphOcclusionVisitor visitorRegular;

    @WrapOperation(method = "addNearbySections", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;isWithinNearbySectionFrustum(Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)Z"))
    private boolean txoptimizations$keepNearbyOutsideFrustum(Viewport viewport, RenderSection section, Operation<Boolean> original) {
        if (original.call(viewport, section))
            return true;

        this.visitorWide.visit(section, false);
        this.visitorRegular.visit(section, false);

        return false;
    }
}
