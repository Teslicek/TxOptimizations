package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.Objects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Unique
    private boolean txoptimizations$pushing;

    @Inject(method = "pushDescriptors", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipUnchangedPush(CallbackInfo ci) {
        this.txoptimizations$pushing = false;

        if (!this.anyDescriptorDirty)
            return;

        if (this.txoptimizations$matchesPushed()) {
            this.anyDescriptorDirty = false;
            ci.cancel();
            return;
        }

        this.txoptimizations$pushing = true;
    }

    @Inject(method = "pushDescriptors", at = @At("TAIL"))
    private void txoptimizations$rememberPush(CallbackInfo ci) {
        if (!this.txoptimizations$pushing)
            return;

        this.txoptimizations$pushedUniforms = this.uniforms.toArray();
        this.txoptimizations$pushedLayout   = this.pipeline.pipelineLayout();
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
