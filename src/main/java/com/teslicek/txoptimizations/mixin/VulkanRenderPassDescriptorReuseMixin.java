package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.Objects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassDescriptorReuseMixin {

    @Shadow
    protected VulkanRenderPipeline pipeline;

    @Shadow
    private boolean anyDescriptorDirty;

    @Shadow
    @Final
    protected ReferenceList<Object> uniforms;

    @Unique
    private Object[] txoptimizations$pushedUniforms;

    @Unique
    private long txoptimizations$pushedLayout;

    @WrapWithCondition(method = {"drawIndexed", "multiDrawIndexed", "drawIndexedIndirect", "draw", "multiDraw", "drawIndirect"}, at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanRenderPass;pushDescriptors()V"))
    private boolean txoptimizations$pushChangedDescriptors(VulkanRenderPass pass) {
        if (!this.anyDescriptorDirty)
            return false;

        if (this.txoptimizations$matchesPushed()) {
            this.anyDescriptorDirty = false;

            return false;
        }

        int size = this.uniforms.size();

        if (this.txoptimizations$pushedUniforms == null || this.txoptimizations$pushedUniforms.length != size)
            this.txoptimizations$pushedUniforms = new Object[size];

        this.uniforms.getElements(0, this.txoptimizations$pushedUniforms, 0, size);
        this.txoptimizations$pushedLayout = this.pipeline.pipelineLayout();

        return true;
    }

    @Unique
    private boolean txoptimizations$matchesPushed() {
        Object[] pushed = this.txoptimizations$pushedUniforms;

        if (pushed == null || pushed.length != this.uniforms.size() || this.txoptimizations$pushedLayout != this.pipeline.pipelineLayout())
            return false;

        for (int index = 0; index < pushed.length; index ++) {
            if (!Objects.equals(pushed[index], this.uniforms.get(index)))
                return false;
        }

        return true;
    }
}
