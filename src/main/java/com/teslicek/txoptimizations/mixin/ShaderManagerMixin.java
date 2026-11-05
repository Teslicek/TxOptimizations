package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.LazyPipelines;
import com.teslicek.txoptimizations.PackedTerrain;
import com.teslicek.txoptimizations.RgssShortcut;
import com.teslicek.txoptimizations.ShaderIncludesDigest;
import com.teslicek.txoptimizations.SkyFarPlane;
import com.teslicek.txoptimizations.SpirvCache;
import java.util.List;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {

    @Inject(method = "loadConfigs", at = @At("HEAD"))
    private static void txoptimizations$startIncludes(CallbackInfoReturnable<ShaderManager.Configs> cir) {
        SpirvCache.startIncludes();
    }

    @WrapOperation(method = "loadInclude", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/pipeline/ShaderSource$CachedIncludeSource;create(Lnet/minecraft/resources/Identifier;Ljava/lang/String;)Lcom/mojang/renderpearl/api/pipeline/ShaderSource$CachedIncludeSource;"))
    private static ShaderSource.CachedIncludeSource txoptimizations$collectInclude(Identifier id, String contents, Operation<ShaderSource.CachedIncludeSource> original) {
        SpirvCache.collectInclude(id, contents);

        return original.call(id, contents);
    }

    @ModifyReturnValue(method = "loadConfigs", at = @At("RETURN"))
    private static ShaderManager.Configs txoptimizations$digestIncludes(ShaderManager.Configs configs) {
        ((ShaderIncludesDigest) (Object) configs).txoptimizations$setIncludesDigest(SpirvCache.finishIncludes());

        return configs;
    }

    @Inject(method = "reload", at = @At("HEAD"))
    private void txoptimizations$rememberLazyPipelines(CallbackInfoReturnable<?> cir) {
        LazyPipelines.remember();
    }

    @WrapOperation(method = "lambda$reload$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderPipelines;optionalPipelines()Ljava/util/List;"))
    private static List<RenderPipeline> txoptimizations$compileLazyPipelines(Operation<List<RenderPipeline>> original) {
        return LazyPipelines.withRemembered(original.call());
    }

    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String txoptimizations$patchSources(String contents, @Local(argsOnly = true) Identifier location) {
        return GpuCubes.patch(location, PackedTerrain.patch(location, SkyFarPlane.patch(location, contents)));
    }

    @ModifyExpressionValue(method = "loadInclude", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String txoptimizations$patchIncludes(String contents, @Local(argsOnly = true) Identifier location) {
        return RgssShortcut.patch(location, contents);
    }
}
