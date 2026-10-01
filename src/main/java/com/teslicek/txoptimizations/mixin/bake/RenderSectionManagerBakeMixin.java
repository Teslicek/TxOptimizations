package com.teslicek.txoptimizations.mixin.bake;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.bake.ChunkTasks;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSectionManager.class)
public abstract class RenderSectionManagerBakeMixin {

    @WrapOperation(method = "processChunkBuildResults", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;retrievePendingBuildOutput()Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"))
    private ChunkBuildOutput txoptimizations$runSectionTasks(RenderSection section, Operation<ChunkBuildOutput> original) {
        ChunkTasks.run(section.getPosition());

        return original.call(section);
    }
}
