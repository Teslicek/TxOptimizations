package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelExtractor.class)
public interface LevelExtractorSectionDirtyInvoker {

    @Invoker("setSectionDirty")
    void txoptimizations$setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean playerChanged);
}
