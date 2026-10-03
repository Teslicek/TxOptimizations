package com.teslicek.txoptimizations.mixin.gpu;

import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKIndirectDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VKIndirectDrawBatch.class)
public abstract class VKIndirectDrawBatchProfilerMixin extends MultiDrawBatch {

    @Shadow
    @Final
    private long pCommands;

    @Inject(method = "draw", at = @At("HEAD"))
    private void txoptimizations$countTerrainDraws(DrawContext context, CallbackInfo ci) {
        if (!GpuPassProfiler.isRecording())
            return;

        long indices = 0L;

        for (int index = 0; index < this.size; index ++)
            indices += MemoryUtil.memGetInt(this.pCommands + (long) index * VkDrawIndexedIndirectCommand.SIZEOF + VkDrawIndexedIndirectCommand.INDEXCOUNT);

        GpuPassProfiler.countTerrainDraws(this.size, indices);
    }
}
