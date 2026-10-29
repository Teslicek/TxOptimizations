package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.ChunkTasks;
import com.teslicek.txoptimizations.bake.SectionTasks;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionCompiler.Results.class)
public abstract class SectionCompilerResultsBakeMixin implements SectionTasks {

    @Unique
    private final Queue<Runnable> txoptimizations$sectionTasks = new ConcurrentLinkedQueue<>();

    @Unique
    private ChunkTasks.@Nullable Claim txoptimizations$claim;

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
