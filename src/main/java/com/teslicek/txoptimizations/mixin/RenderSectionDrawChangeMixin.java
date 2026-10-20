package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.teslicek.txoptimizations.TerrainDrawChanges;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionDrawChangeMixin {

    @ModifyReturnValue(method = "setSectionMesh", at = @At("RETURN"))
    private SectionMesh txoptimizations$markMeshSet(SectionMesh oldMesh) {
        TerrainDrawChanges.mark();

        return oldMesh;
    }

    @ModifyExpressionValue(method = "reset", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$markMeshReset(Object oldMesh) {
        TerrainDrawChanges.mark();

        return oldMesh;
    }
}
