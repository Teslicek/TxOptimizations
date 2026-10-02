package com.teslicek.txoptimizations.mixin.cull;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.cull.OccluderBoxes;
import com.teslicek.txoptimizations.cull.SectionOccluders;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.DirectionalVisGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskOccluderMixin {

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;build()Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo;"))
    private BuiltSectionInfo txoptimizations$attachOccluders(BuiltSectionInfo.Builder builder, Operation<BuiltSectionInfo> original, @Local DirectionalVisGraph occluder) {
        BuiltSectionInfo info = original.call(builder);

        ((SectionOccluders) info).txoptimizations$setOccluders(OccluderBoxes.build(((DirectionalVisGraphAccessor) occluder).txoptimizations$blocks()));

        return info;
    }
}
