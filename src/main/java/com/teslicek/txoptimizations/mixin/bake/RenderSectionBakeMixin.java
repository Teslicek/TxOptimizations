package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.ChunkTasks;
import com.teslicek.txoptimizations.bake.SectionTasks;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionBakeMixin {

    @ModifyVariable(method = "setSectionMesh", at = @At("HEAD"), argsOnly = true)
    private SectionMesh txoptimizations$runSectionTasks(SectionMesh sectionMesh) {
        if (sectionMesh instanceof SectionTasks tasks)
            ChunkTasks.run(tasks.txoptimizations$getSectionTasks());

        return sectionMesh;
    }
}
