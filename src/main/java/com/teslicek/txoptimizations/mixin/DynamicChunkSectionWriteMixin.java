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

@Mixin(value = DynamicGpuData.ChunkSectionInfo.class, priority = 999)
public abstract class DynamicChunkSectionWriteMixin {

    @Unique
    private static final int BYTES = 3 * Integer.BYTES + Float.BYTES;

    @Shadow
    @Final
    private int x;

    @Shadow
    @Final
    private int y;

    @Shadow
    @Final
    private int z;

    @Shadow
    @Final
    private float visibility;

    @Overwrite
    public void write(ByteBuffer buffer) {
        long address = NativeWrites.reserve(buffer, BYTES);

        MemoryUtil.memPutInt(address, this.x);
        MemoryUtil.memPutInt(address + 4L, this.y);
        MemoryUtil.memPutInt(address + 8L, this.z);
        MemoryUtil.memPutFloat(address + 12L, this.visibility);
    }
}
