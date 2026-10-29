package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.ChunkTasks;
import com.teslicek.txoptimizations.bake.SectionTasks;
import java.util.Queue;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshBakeMixin implements SectionTasks {

    @Unique
    private Queue<Runnable> txoptimizations$sectionTasks;

    @Unique
    private ChunkTasks.@Nullable Claim txoptimizations$claim;

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;J)V", at = @At("TAIL"))
    private void txoptimizations$takeSectionTasks(TranslucencyPointOfView pointOfView, SectionCompiler.Results results, long compileTaskStartTimeNs, CallbackInfo ci) {
        SectionTasks tasks = (SectionTasks) (Object) results;

        this.txoptimizations$sectionTasks = tasks.txoptimizations$getSectionTasks();
        this.txoptimizations$claim        = tasks.txoptimizations$getClaim();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void txoptimizations$restoreUnrunClaim(CallbackInfo ci) {
        ChunkTasks.restore(this);
    }

    @Override
    public Queue<Runnable> txoptimizations$getSectionTasks() {
        return this.txoptimizations$sectionTasks;
    }

    @Override
    public ChunkTasks.@Nullable Claim txoptimizations$getClaim() {
        return this.txoptimizations$claim;
    }

    @Override
    public void txoptimizations$setClaim(ChunkTasks.@Nullable Claim claim) {
        this.txoptimizations$claim = claim;
    }
}
