package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DrawRecordHolder;
import com.teslicek.txoptimizations.SectionDrawRecord;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionDrawRecordMixin implements SectionDrawRecord {

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
        } while (this.getSectionMesh() != mesh);
    }
}
