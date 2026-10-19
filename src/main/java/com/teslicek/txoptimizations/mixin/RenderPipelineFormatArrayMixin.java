package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderPipeline.class, priority = 999)
public abstract class RenderPipelineFormatArrayMixin {

    @Shadow
    @Final
    private List<@Nullable VertexFormat> vertexFormatPerBuffer;

    @Unique
    private @Nullable VertexFormat[] txoptimizations$formats;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$copyFormats(CallbackInfo ci) {
        this.txoptimizations$formats = this.vertexFormatPerBuffer.toArray(new VertexFormat[0]);
    }

    @Overwrite
    public @Nullable VertexFormat getVertexFormatBinding(int bindingIndex) {
        return this.txoptimizations$formats[bindingIndex];
    }
}
