package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.SkyFarPlane;
import com.teslicek.txoptimizations.TerrainRgssSkip;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerSourcePatchMixin {

    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"))
    private static String txoptimizations$patchSources(String contents, @Local(argsOnly = true) Identifier location) {
        return TerrainRgssSkip.patch(location, SkyFarPlane.patch(location, contents));
    }
}
