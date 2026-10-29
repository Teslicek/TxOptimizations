package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.SectionTasks;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionCompiler.Results.class)
public abstract class SectionCompilerResultsBakeMixin implements SectionTasks {

    @Unique
    private final Queue<Runnable> txoptimizations$sectionTasks = new ConcurrentLinkedQueue<>();

    @Override
    public Queue<Runnable> txoptimizations$getSectionTasks() {
        return this.txoptimizations$sectionTasks;
    }
}
