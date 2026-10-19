package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionMark;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionSubsetMarkMixin implements SectionMark {

    @Unique
    private int txoptimizations$subsetMark;

    @Override
    public int txoptimizations$getSubsetMark() {
        return this.txoptimizations$subsetMark;
    }

    @Override
    public void txoptimizations$setSubsetMark(int mark) {
        this.txoptimizations$subsetMark = mark;
    }
}
