package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.ChunkTasks;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionBakeMixin {

    @Shadow
    private volatile long sectionNode;

    @ModifyVariable(method = "setSectionMesh", at = @At("HEAD"), argsOnly = true)
    private SectionMesh txoptimizations$runSectionTasks(SectionMesh sectionMesh) {
        ChunkTasks.run(this.sectionNode);

        return sectionMesh;
    }
}
