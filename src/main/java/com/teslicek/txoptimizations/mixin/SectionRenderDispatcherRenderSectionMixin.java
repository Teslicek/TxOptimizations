package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DeferredMeshRelease;
import com.teslicek.txoptimizations.DrawRecordHolder;
import com.teslicek.txoptimizations.DrawRecords;
import com.teslicek.txoptimizations.NeighborMark;
import com.teslicek.txoptimizations.SectionDrawRecord;
import com.teslicek.txoptimizations.SectionMark;
import com.teslicek.txoptimizations.SectionMeshReleases;
import com.teslicek.txoptimizations.SectionSubset;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class SectionRenderDispatcherRenderSectionMixin implements SectionDrawRecord, SectionMark, NeighborMark, Octree.Node, DeferredMeshRelease {

    @Redirect(method = "setSectionMesh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$countBlockEntityMesh(AtomicReference<SectionMesh> sectionMesh, Object mesh) {
        SectionMesh next = (SectionMesh) mesh;

        if (next.getRenderableBlockEntities().isEmpty())
            return sectionMesh.getAndSet(next);

        SectionSubset.BLOCK_ENTITIES.beginChange();

        SectionMesh previous = sectionMesh.getAndSet(next);

        SectionSubset.BLOCK_ENTITIES.endChange();

        return previous;
    }

    @Unique
    private volatile int @Nullable [] txoptimizations$drawRecord;

    @Unique
    private volatile boolean txoptimizations$translucentGeometry;

    @Shadow
    public abstract SectionMesh getSectionMesh();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$readInitialRecord(CallbackInfo ci) {
        this.txoptimizations$readRecord();
    }

    @Inject(method = "setSectionMesh", at = @At("RETURN"))
    private void txoptimizations$readNewRecord(CallbackInfoReturnable<?> cir) {
        this.txoptimizations$readRecord();
    }

    @Inject(method = "reset", at = @At("RETURN"))
    private void txoptimizations$readResetRecord(CallbackInfo ci) {
        this.txoptimizations$readRecord();
    }

    @Override
    public int @Nullable [] txoptimizations$sectionDrawRecord() {
        return this.txoptimizations$drawRecord;
    }

    @Override
    public boolean txoptimizations$hasTranslucentGeometry() {
        return this.txoptimizations$translucentGeometry;
    }

    @Unique
    private void txoptimizations$readRecord() {
        SectionMesh mesh;

        do {
            mesh                                     = this.getSectionMesh();
            this.txoptimizations$drawRecord          = mesh instanceof DrawRecordHolder holder ? holder.txoptimizations$drawRecord() : null;
            this.txoptimizations$translucentGeometry = mesh.hasTranslucentGeometry();
            DrawRecords.recordReplaced();
        } while (this.getSectionMesh() != mesh);
    }

    @Unique
    private int txoptimizations$subsetMark;

    @Override
    public int txoptimizations$getSubsetMark() {
        return this.txoptimizations$subsetMark;
    }

    @Override
    public void txoptimizations$setSubsetMark(int mark) {
        this.txoptimizations$subsetMark = mark;
    }

    @Unique
    private long txoptimizations$missingNeighborsNode;

    @Unique
    private int txoptimizations$missingNeighborsGeneration;

    @Override
    public long txoptimizations$getMissingNeighborsNode() {
        return this.txoptimizations$missingNeighborsNode;
    }

    @Override
    public int txoptimizations$getMissingNeighborsGeneration() {
        return this.txoptimizations$missingNeighborsGeneration;
    }

    @Override
    public void txoptimizations$markMissingNeighbors(long sectionNode, int generation) {
        this.txoptimizations$missingNeighborsNode       = sectionNode;
        this.txoptimizations$missingNeighborsGeneration = generation;
    }

    @Shadow
    public abstract AABB getBoundingBox();

    @Override
    public void visitNodes(Octree.OctreeVisitor visitor, boolean skipFrustumCheck, Frustum frustum, int depth, int closeDistance, boolean isClose) {
        throw new UnsupportedOperationException("Octree leaf sections are visited by the branch that holds them");
    }

    @Override
    public SectionRenderDispatcher.RenderSection getSection() {
        return (SectionRenderDispatcher.RenderSection) (Object) this;
    }

    @Override
    public AABB getAABB() {
        return this.getBoundingBox();
    }

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
