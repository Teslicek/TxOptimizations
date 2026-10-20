package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionMeshReleases;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMeshReleaseMixin implements SectionMeshReleases {

    @Unique
    private final ConcurrentLinkedQueue<Runnable> txoptimizations$releases = new ConcurrentLinkedQueue<>();

    @Override
    public void txoptimizations$releaseOnRenderThread(Runnable release) {
        this.txoptimizations$releases.add(release);
    }

    @Inject(method = "uploadTerrainBuffersToGpu", at = @At("HEAD"))
    private void txoptimizations$runReleases(CallbackInfo ci) {
        Runnable release;

        while ((release = this.txoptimizations$releases.poll()) != null)
            release.run();
    }
}
