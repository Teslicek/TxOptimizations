package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.bake.ChunkTasks;
import com.teslicek.txoptimizations.bake.SectionTasks;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public abstract class SectionCompileTaskBakeMixin {

    @Inject(method = "doTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setSectionMesh(Lnet/minecraft/client/renderer/chunk/SectionMesh;)Lnet/minecraft/client/renderer/chunk/SectionMesh;"), require = 1)
    private void txoptimizations$runTasksOfEmptyMesh(SectionBufferBuilderPack buffers, CallbackInfoReturnable<?> cir, @Local CompiledSectionMesh compiledSectionMesh) {
        ChunkTasks.run(((SectionTasks) compiledSectionMesh).txoptimizations$getSectionTasks());
    }
}
