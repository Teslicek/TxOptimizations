package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.LazyPipelines;
import com.teslicek.txoptimizations.PackedTerrain;
import com.teslicek.txoptimizations.RgssShortcut;
import com.teslicek.txoptimizations.ShaderIncludesDigest;
import com.teslicek.txoptimizations.SkyFarPlane;
import com.teslicek.txoptimizations.SpirvCache;
import com.teslicek.txoptimizations.hud.HudShaderOverrides;
import java.util.List;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {

    @Inject(method = "loadConfigs", at = @At("HEAD"))
    private static void txoptimizations$startIncludes(CallbackInfoReturnable<ShaderManager.Configs> cir) {
        SpirvCache.startIncludes();
        HudShaderOverrides.startLoading();
    }

    @Inject(method = "loadShader", at = @At("HEAD"))
    private static void txoptimizations$collectOverride(Identifier location, Resource resource, ShaderType type, ImmutableMap.Builder<?, String> output, CallbackInfo ci) {
        HudShaderOverrides.collect(location, resource, type);
    }

    @WrapOperation(method = "loadInclude", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/pipeline/ShaderSource$CachedIncludeSource;create(Lnet/minecraft/resources/Identifier;Ljava/lang/String;)Lcom/mojang/renderpearl/api/pipeline/ShaderSource$CachedIncludeSource;"))
    private static ShaderSource.CachedIncludeSource txoptimizations$collectInclude(Identifier id, String contents, Operation<ShaderSource.CachedIncludeSource> original) {
        SpirvCache.collectInclude(id, contents);

        return original.call(id, contents);
    }

    @ModifyReturnValue(method = "loadConfigs", at = @At("RETURN"))
    private static ShaderManager.Configs txoptimizations$digestIncludes(ShaderManager.Configs configs) {
        ((ShaderIncludesDigest) (Object) configs).txoptimizations$setIncludesDigest(SpirvCache.finishIncludes());
        HudShaderOverrides.finishLoading();

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
