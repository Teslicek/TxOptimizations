package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BatchVersion;
import com.teslicek.txoptimizations.IndirectCommandSlots;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKIndirectDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.gpu.device.context.VKIndirectContext;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VKIndirectDrawBatch.class, remap = false)
public abstract class VKIndirectDrawBatchReuseMixin extends MultiDrawBatch {

    @Shadow
    @Final
    private long pCommands;

    @Shadow
    private long offset;

    @Overwrite
    public void prepare(DrawContext dc) {
        VKIndirectContext context  = (VKIndirectContext) dc;
        int               byteSize = this.size * VkDrawIndexedIndirectCommand.SIZEOF;
        long              offset   = context.addCommand(byteSize);

        if (!((IndirectCommandSlots) context).txoptimizations$isWritten(this, ((BatchVersion) this).txoptimizations$getVersion()))
            MemoryUtil.memCopy(this.pCommands, MemoryUtil.memAddress(context.mappedView.data()) + offset, byteSize);

        this.offset = offset;
    }
}
