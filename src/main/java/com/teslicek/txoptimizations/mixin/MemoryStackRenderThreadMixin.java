package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderThreadStack;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MemoryStack.class, remap = false)
public abstract class MemoryStackRenderThreadMixin {

    @Shadow
    @Final
    private static ThreadLocal<MemoryStack> TLS;

    @Overwrite
    public static MemoryStack stackGet() {
        RenderThreadStack.Owner owner = RenderThreadStack.owner();

        if (owner != null && owner.thread() == Thread.currentThread())
            return owner.stack();

        return TLS.get();
    }
}
