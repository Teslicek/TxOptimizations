package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.SectionAllocations;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshUploadFlagsMixin {

    @Unique
    private static final int CLOSED = 1 << 31;

    @Unique
    private final AtomicInteger txoptimizations$uploaded = new AtomicInteger();

    @Redirect(method = "<init>(Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;makeEnumMap(Ljava/lang/Class;Ljava/util/function/Function;)Ljava/util/Map;"), require = 2)
    private <K extends Enum<K>, V> Map<K, V> txoptimizations$skipUploadFlagMaps(Class<K> keyType, Function<K, V> function) {
        return null;
    }

    @WrapOperation(method = "close", at = {@At(value = "INVOKE", target = "Ljava/util/Map;clear()V", ordinal = 1), @At(value = "INVOKE", target = "Ljava/util/Map;clear()V", ordinal = 2)}, require = 2)
    private void txoptimizations$closeUploadFlags(Map<?, ?> uploadFlags, Operation<Void> original) {
        this.txoptimizations$uploaded.getAndAccumulate(CLOSED, (flags, closed) -> flags | closed);
    }

    @Overwrite
    public boolean isVertexBufferUploaded(ChunkSectionLayer layer) {
        return this.txoptimizations$isUploaded(SectionAllocations.vertexSlot(layer));
    }

    @Overwrite
    public boolean isIndexBufferUploaded(ChunkSectionLayer layer) {
        return this.txoptimizations$isUploaded(SectionAllocations.indexSlot(layer));
    }

    @Overwrite
    public void setVertexBufferUploaded(ChunkSectionLayer layer) {
        this.txoptimizations$setUploaded(SectionAllocations.vertexSlot(layer));
    }

    @Overwrite
    public void setIndexBufferUploaded(ChunkSectionLayer layer) {
        this.txoptimizations$setUploaded(SectionAllocations.indexSlot(layer));
    }

    @Unique
    private boolean txoptimizations$isUploaded(int slot) {
        int flags = this.txoptimizations$uploaded.get();

        if ((flags & CLOSED) != 0)
            throw new NullPointerException("Read the upload state of slot " + slot + " of a closed section mesh");

        return (flags & 1 << slot) != 0;
    }

    @Unique
    private void txoptimizations$setUploaded(int slot) {
        int flags = this.txoptimizations$uploaded.getAndAccumulate(1 << slot, (current, uploaded) -> (current & CLOSED) != 0 ? current : current | uploaded);

        if ((flags & CLOSED) != 0)
            throw new NullPointerException("Marked slot " + slot + " of a closed section mesh as uploaded");
    }
}
