package com.teslicek.txoptimizations.mixin;

import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StagedVertexBuffer.class)
public interface StagedVertexBufferListAccessor {

    @Accessor("draws")
    List<StagedVertexBuffer.Draw> txoptimizations$draws();

    @Invoker("finishLastVertexBuilder")
    void txoptimizations$finishLastVertexBuilder();
}
