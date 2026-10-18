package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.SectionSubset;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionBlockEntityMeshMixin {

    @WrapOperation(method = "setSectionMesh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$countBlockEntityMesh(AtomicReference<SectionMesh> sectionMesh, Object mesh, Operation<Object> original) {
        if (((SectionMesh) mesh).getRenderableBlockEntities().isEmpty())
            return original.call(sectionMesh, mesh);

        SectionSubset.BLOCK_ENTITIES.beginChange();

        Object previous = original.call(sectionMesh, mesh);

        SectionSubset.BLOCK_ENTITIES.endChange();

        return previous;
    }
}
