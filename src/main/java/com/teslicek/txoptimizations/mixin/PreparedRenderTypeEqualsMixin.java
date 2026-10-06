package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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

@Mixin(PreparedRenderType.class)
public abstract class PreparedRenderTypeEqualsMixin {

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

    @Unique
    private int txoptimizations$hash;

    @Unique
    private boolean txoptimizations$hashed;

    @WrapMethod(method = "hashCode")
    private int txoptimizations$hashOnce(Operation<Integer> original) {
        if (!this.txoptimizations$hashed) {
            this.txoptimizations$hash   = original.call();
            this.txoptimizations$hashed = true;
        }

        return this.txoptimizations$hash;
    }

    @Overwrite
    public final boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof PreparedRenderType that))
            return false;

        return this.pipeline == that.pipeline() && Objects.equals(this.dynamicTransforms, that.dynamicTransforms()) && Objects.equals(this.scissorState, that.scissorState()) && Objects.equals(this.oitPipelineSet, that.oitPipelineSet()) && Objects.equals(this.name, that.name()) && Objects.equals(this.textures, that.textures());
    }
}
