package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DeferredMeshRelease;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public abstract class SectionCompileTaskMeshReleaseMixin {

    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;releaseSectionMesh(Lnet/minecraft/client/renderer/chunk/SectionMesh;)V"), require = 2)
    private void txoptimizations$releaseOnRenderThread(SectionRenderDispatcher.RenderSection section, SectionMesh mesh) {
        ((DeferredMeshRelease) section).txoptimizations$releaseLater(mesh);
    }

    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;lock()V"), require = 2)
    private void txoptimizations$skipReleaseLock(ReentrantLock copyLock) {
    }

    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;unlock()V"), require = 4)
    private void txoptimizations$skipReleaseUnlock(ReentrantLock copyLock) {
    }
}
