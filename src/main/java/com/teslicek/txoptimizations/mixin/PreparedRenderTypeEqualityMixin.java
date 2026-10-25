package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PreparedRenderType.class, priority = 999)
public abstract class PreparedRenderTypeEqualityMixin {

    @Unique
    private static final int PRIME = 31;

    @Shadow
    @Final
    private String name;

    @Shadow
    @Final
    private RenderPipeline pipeline;

    @Shadow
    @Final
    private OitPipelineSet oitPipelineSet;

    @Shadow
    @Final
    private GpuBufferSlice dynamicTransforms;

    @Shadow
    @Final
    private ScissorState scissorState;

    @Shadow
    @Final
    private List<PreparedRenderType.Texture> textures;

    @Overwrite
    public final boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof PreparedRenderType that))
            return false;

        return this.pipeline == that.pipeline() && Objects.equals(this.dynamicTransforms, that.dynamicTransforms()) && Objects.equals(this.scissorState, that.scissorState()) && Objects.equals(this.oitPipelineSet, that.oitPipelineSet()) && Objects.equals(this.name, that.name()) && Objects.equals(this.textures, that.textures());
    }

    @Overwrite
    public final int hashCode() {
        int hash = Objects.hashCode(this.name);

        hash = hash * PRIME + Objects.hashCode(this.pipeline);
        hash = hash * PRIME + txoptimizations$hash(this.oitPipelineSet);
        hash = hash * PRIME + Objects.hashCode(this.dynamicTransforms);
        hash = hash * PRIME + Objects.hashCode(this.scissorState);

        return hash * PRIME + Objects.hashCode(this.textures);
    }

    @Unique
    private static int txoptimizations$hash(OitPipelineSet pipelines) {
        if (pipelines == null)
            return 0;

        int hash = Objects.hashCode(pipelines.depthBoundsPipeline());

        hash = hash * PRIME + Objects.hashCode(pipelines.transmittancePipeline());

        return hash * PRIME + Objects.hashCode(pipelines.accumulatePipeline());
    }
}
