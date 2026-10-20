package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.StagingBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.teslicek.txoptimizations.SectionMeshReleases;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMeshReleaseMixin implements SectionMeshReleases {

    @Unique
    private final ConcurrentLinkedQueue<Runnable> txoptimizations$releases = new ConcurrentLinkedQueue<>();

    @Override
    public void txoptimizations$releaseOnRenderThread(Runnable release) {
        this.txoptimizations$releases.add(release);
    }

    @Redirect(method = "uploadTerrainBuffersToGpu", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/StagingBuffer;startUploading(Lcom/mojang/renderpearl/api/commands/CommandEncoder;)Lcom/mojang/blaze3d/vertex/StagingBuffer$Uploader;"))
    private StagingBuffer.Uploader txoptimizations$releaseBeforeUploading(StagingBuffer stagingBuffer, CommandEncoder encoder) {
        Runnable release;

        while ((release = this.txoptimizations$releases.poll()) != null)
            release.run();

        return stagingBuffer.startUploading(encoder);
    }
}
