package com.teslicek.txoptimizations.mixin.bake;

import com.teslicek.txoptimizations.bake.ChunkTasks;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionBakeMixin {

    @Shadow
    private volatile long sectionNode;

    @Inject(method = "setSectionMesh", at = @At("HEAD"))
    private void txoptimizations$runSectionTasks(SectionMesh sectionMesh, CallbackInfoReturnable<SectionMesh> cir) {
        ChunkTasks.run(this.sectionNode);
    }
}
