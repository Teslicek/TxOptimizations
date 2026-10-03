package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.MergedDrawBatch;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.VKIndirectDrawBatch;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VKIndirectDrawBatch.class)
public abstract class VKIndirectDrawBatchMergeMixin extends MultiDrawBatch implements MergedDrawBatch {

    @Unique
    private static final int STRIDE = VkDrawIndexedIndirectCommand.SIZEOF;

    @Unique
    private static final int INDEX_COUNT = VkDrawIndexedIndirectCommand.INDEXCOUNT;

    @Unique
    private static final int FIRST_INDEX = VkDrawIndexedIndirectCommand.FIRSTINDEX;

    @Unique
    private static final int VERTEX_OFFSET = VkDrawIndexedIndirectCommand.VERTEXOFFSET;

    @Shadow
    @Final
    private long pCommands;

    @Unique
    private int txoptimizations$unmerged;

    @Override
    public void txoptimizations$mergeContiguous(int maxElementCount, boolean merge) {
        int count = this.size;

        this.txoptimizations$unmerged = count;

        if (!merge || count < 2)
            return;

        int  write       = 0;
        long last        = 0L;
        int  lastIndices = 0;
        int  lastFirst   = 0;
        int  lastVertex  = 0;

        for (int read = 0; read < count; read ++) {
            long source  = this.pCommands + (long) read * STRIDE;
            int  indices = MemoryUtil.memGetInt(source + INDEX_COUNT);
            int  first   = MemoryUtil.memGetInt(source + FIRST_INDEX);
            int  vertex  = MemoryUtil.memGetInt(source + VERTEX_OFFSET);

            if (write > 0 && first == lastFirst && vertex == lastVertex + lastIndices / 6 * 4 && (long) lastIndices + indices <= maxElementCount) {
                lastIndices += indices;
                MemoryUtil.memPutInt(last + INDEX_COUNT, lastIndices);
                this.updateMaxElementCount(lastIndices);
                continue;
            }

            last = this.pCommands + (long) write * STRIDE;

            if (write != read) {
                MemoryUtil.memPutInt(last + INDEX_COUNT, indices);
                MemoryUtil.memPutInt(last + FIRST_INDEX, first);
                MemoryUtil.memPutInt(last + VERTEX_OFFSET, vertex);
            }

            lastIndices = indices;
            lastFirst   = first;
            lastVertex  = vertex;
            write ++;
        }

        this.size = write;
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void txoptimizations$countDraws(DrawContext context, CallbackInfo ci) {
        GpuPassProfiler.countTerrainDraws(this.size, this.txoptimizations$unmerged);
    }
}
