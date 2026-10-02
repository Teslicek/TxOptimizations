package com.teslicek.txoptimizations.mixin.cull;

import com.teslicek.txoptimizations.cull.SectionOccluders;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BuiltSectionInfo.class)
public abstract class BuiltSectionInfoOccluderMixin implements SectionOccluders {

    @Unique
    private int[] txoptimizations$occluders;

    @Override
    public int[] txoptimizations$getOccluders() {
        return this.txoptimizations$occluders;
    }

    @Override
    public void txoptimizations$setOccluders(int[] boxes) {
        this.txoptimizations$occluders = boxes;
    }
}
