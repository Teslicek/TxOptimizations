package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelExtractor.class)
public interface LevelExtractorStateAccessor {

    @Accessor("levelRenderState")
    @Mutable
    void txoptimizations$setLevelRenderState(LevelRenderState value);
}
