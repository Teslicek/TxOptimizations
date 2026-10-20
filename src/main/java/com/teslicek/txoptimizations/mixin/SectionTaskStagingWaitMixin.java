package com.teslicek.txoptimizations.mixin;

import java.util.concurrent.locks.LockSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {"net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask", "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask"})
public abstract class SectionTaskStagingWaitMixin {

    @Unique
    private static final long STAGING_RETRY_NANOS = 50_000L;

    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"))
    private void txoptimizations$waitForStagingSpace() {
        LockSupport.parkNanos(STAGING_RETRY_NANOS);
    }
}
