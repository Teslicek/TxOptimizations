package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.teslicek.txoptimizations.ShaderIncludesDigest;
import com.teslicek.txoptimizations.SpirvCache;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerIncludesDigestMixin {

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
}
