package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.IndexType;
import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StagedVertexBuffer.Draw.class)
public interface StagedVertexBufferDrawAccessor {

    @Accessor("vertexBufferSlices")
    List<ByteBufferBuilder.Result> txoptimizations$vertexBufferSlices();

    @Accessor("vertexOffset")
    int txoptimizations$vertexOffset();

    @Accessor("indexOffset")
    int txoptimizations$indexOffset();

    @Accessor("quadSorting")
    VertexSorting txoptimizations$quadSorting();

    @Invoker("indexType")
    IndexType txoptimizations$indexType();

    @Invoker("freeVertexData")
    void txoptimizations$freeVertexData();
}
