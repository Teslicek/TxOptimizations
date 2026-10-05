package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import com.teslicek.txoptimizations.SpirvCache;
import java.nio.file.Path;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlslCompiler.class)
public abstract class GlslCompilerSpirvCacheMixin {

    @Shadow
    @Final
    private boolean isZeroToOne;

    @Shadow
    @Final
    private boolean shaderDrawParameters;

    @WrapMethod(method = "compileToSpv")
    private SpvModule txoptimizations$cacheSpirv(String name, String source, ShaderType type, ShaderDefines shaderDefines, ShaderSource shaderSource, Operation<SpvModule> original) {
        if (!(shaderSource instanceof ShaderManager.Configs configs))
            return original.call(name, source, type, shaderDefines, shaderSource);

        Path file = SpirvCache.file(this.isZeroToOne, this.shaderDrawParameters, name, source, type, shaderDefines, configs);

        return SpirvCache.load(file, type, () -> original.call(name, source, type, shaderDefines, shaderSource));
    }
}
