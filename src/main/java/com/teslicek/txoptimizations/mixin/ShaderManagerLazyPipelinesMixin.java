package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.LazyPipelines;
import java.util.List;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerLazyPipelinesMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    private void txoptimizations$rememberLazyPipelines(CallbackInfoReturnable<?> cir) {
        LazyPipelines.remember();
    }

    @WrapOperation(method = "lambda$reload$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderPipelines;optionalPipelines()Ljava/util/List;"))
    private static List<RenderPipeline> txoptimizations$compileLazyPipelines(Operation<List<RenderPipeline>> original) {
        return LazyPipelines.withRemembered(original.call());
    }
}
