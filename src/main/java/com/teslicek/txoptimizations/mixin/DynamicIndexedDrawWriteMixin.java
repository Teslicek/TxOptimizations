package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NativeWrites;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicGpuData;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DynamicGpuData.IndexedDraw.class)
public abstract class DynamicIndexedDrawWriteMixin {

    @Unique
    private static final int BYTES = 5 * Integer.BYTES;

    @Shadow
    @Final
    private int indexCount;

    @Shadow
    @Final
    private int instanceCount;

    @Shadow
    @Final
    private int firstIndex;

    @Shadow
    @Final
    private int baseVertex;

    @Shadow
    @Final
    private int baseInstance;

    @Overwrite
    public void write(ByteBuffer buffer) {
        long address = NativeWrites.reserve(buffer, BYTES);

        MemoryUtil.memPutInt(address, this.indexCount);
        MemoryUtil.memPutInt(address + 4L, this.instanceCount);
        MemoryUtil.memPutInt(address + 8L, this.firstIndex);
        MemoryUtil.memPutInt(address + 12L, this.baseVertex);
        MemoryUtil.memPutInt(address + 16L, this.baseInstance);
    }
}
