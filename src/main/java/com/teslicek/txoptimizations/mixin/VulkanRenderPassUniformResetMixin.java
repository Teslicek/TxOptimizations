package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.util.Arrays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassUniformResetMixin {

    @WrapWithCondition(method = "setPipeline", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ReferenceList;clear()V"))
    private boolean txoptimizations$clearWithResize(ReferenceList<Object> uniforms) {
        if (!(uniforms instanceof ReferenceArrayList<Object>))
            throw new IllegalStateException("Render pass uniforms are a " + uniforms.getClass().getName() + ", not a ReferenceArrayList");

        return false;
    }

    @Redirect(method = "setPipeline", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ReferenceList;size(I)V"))
    private void txoptimizations$resizeEmpty(ReferenceList<Object> uniforms, int size) {
        ReferenceArrayList<Object> list = (ReferenceArrayList<Object>) uniforms;

        Arrays.fill(list.elements(), 0, Math.min(list.size(), size), null);
        list.size(size);
    }
}
