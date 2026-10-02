package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.IndirectCommandUpload;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKIndirectDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VKIndirectDrawBatch.class)
public abstract class VKIndirectDrawBatchDeviceMixin extends MultiDrawBatch {

    @Shadow
    private long offset;

    @Overwrite
    public void draw(DrawContext context) {
        int byteSize = this.size * VkDrawIndexedIndirectCommand.SIZEOF;

        context.getPass().drawIndexedIndirect(((IndirectCommandUpload) context).txoptimizations$deviceCommands().slice(this.offset, byteSize), this.size);
    }
}
