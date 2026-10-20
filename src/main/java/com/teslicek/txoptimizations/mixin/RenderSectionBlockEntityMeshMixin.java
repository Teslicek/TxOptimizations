package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionBlockEntityMeshMixin {

    @Redirect(method = "setSectionMesh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object txoptimizations$countBlockEntityMesh(AtomicReference<SectionMesh> sectionMesh, Object mesh) {
        SectionMesh next = (SectionMesh) mesh;

        if (next.getRenderableBlockEntities().isEmpty())
            return sectionMesh.getAndSet(next);

        SectionSubset.BLOCK_ENTITIES.beginChange();

        SectionMesh previous = sectionMesh.getAndSet(next);

        SectionSubset.BLOCK_ENTITIES.endChange();

        return previous;
    }
}
