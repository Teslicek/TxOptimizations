package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionSubset;
import net.minecraft.client.SectionUpdateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionUpdateTracker.SectionDirtyState.class)
public abstract class SectionDirtyStateChangeMixin {

    @Shadow
    private boolean isDirty;

    @Shadow
    private boolean isDirtyFromPlayer;

    @Overwrite
    public void setDirty(boolean fromPlayer) {
        boolean wasDirty = this.isDirty;

        if (!wasDirty)
            SectionSubset.DIRTY.beginChange();

        this.isDirty           = true;
        this.isDirtyFromPlayer = fromPlayer | (wasDirty && this.isDirtyFromPlayer);

        if (!wasDirty)
            SectionSubset.DIRTY.endChange();
    }
}
