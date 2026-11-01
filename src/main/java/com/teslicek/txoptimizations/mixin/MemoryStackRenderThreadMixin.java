package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderThreadStack;
import com.teslicek.txoptimizations.RenderWorker;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MemoryStack.class, remap = false, priority = 999)
public abstract class MemoryStackRenderThreadMixin {

    @Shadow
    @Final
    private static ThreadLocal<MemoryStack> TLS;

    @Overwrite
    public static MemoryStack stackGet() {
        if (RenderThreadStack.isMain())
            return RenderThreadStack.mainStack();

        if (RenderWorker.isCurrent())
            return RenderWorker.stack();

        return TLS.get();
    }
}
