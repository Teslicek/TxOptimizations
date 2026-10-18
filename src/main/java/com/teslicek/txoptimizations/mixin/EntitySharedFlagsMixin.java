package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SharedFlags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntitySharedFlagsMixin implements SharedFlags {

    @Shadow
    @Final
    protected static EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;

    @Unique
    private byte txoptimizations$sharedFlags;

    @Override
    public void txoptimizations$storeSyncedValue(EntityDataAccessor<?> accessor, Object value) {
        if (accessor == DATA_SHARED_FLAGS_ID)
            this.txoptimizations$sharedFlags = (Byte) value;
    }

    @Overwrite
    protected boolean getSharedFlag(int flag) {
        return (this.txoptimizations$sharedFlags & 1 << flag) != 0;
    }
}
