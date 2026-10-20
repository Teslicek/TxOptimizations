package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DeferredMeshRelease;
import com.teslicek.txoptimizations.SectionMeshReleases;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionMeshReleaseMixin implements DeferredMeshRelease {

    @Shadow
    @Final
    SectionRenderDispatcher this$0;

    @Shadow
    private void releaseSectionMesh(SectionMesh oldMesh) {
        throw new AssertionError();
    }

    @Override
    public void txoptimizations$releaseLater(SectionMesh mesh) {
        ((SectionMeshReleases) this.this$0).txoptimizations$releaseOnRenderThread(() -> this.releaseSectionMesh(mesh));
    }
}
