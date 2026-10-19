package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionCopy.class)
public abstract class SectionCopyEmptyBlockEntitiesMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"))
    private ImmutableMap<BlockPos, BlockEntity> txoptimizations$copyBlockEntities(Map<BlockPos, BlockEntity> blockEntities) {
        if (blockEntities.isEmpty())
            return ImmutableMap.of();

        return ImmutableMap.copyOf(blockEntities);
    }
}
