package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CollisionReach;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionCollisionReachMixin implements CollisionReach {

    @Unique
    private Object txoptimizations$reachData;

    @Unique
    private int txoptimizations$reachPaletteSize;

    @Unique
    private int txoptimizations$reachReads;

    @Unique
    private boolean txoptimizations$reaches;

    @Override
    public boolean txoptimizations$matches(Object data, int paletteSize, int reads) {
        return this.txoptimizations$reachData == data && this.txoptimizations$reachPaletteSize == paletteSize && this.txoptimizations$reachReads == reads;
    }

    @Override
    public boolean txoptimizations$reaches() {
        return this.txoptimizations$reaches;
    }

    @Override
    public void txoptimizations$remember(Object data, int paletteSize, int reads, boolean reaches) {
        this.txoptimizations$reachData        = data;
        this.txoptimizations$reachPaletteSize = paletteSize;
        this.txoptimizations$reachReads       = reads;
        this.txoptimizations$reaches          = reaches;
    }
}
