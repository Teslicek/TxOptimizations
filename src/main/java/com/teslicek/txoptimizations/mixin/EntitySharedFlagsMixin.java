package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SharedFlags;
import java.util.Optional;
import net.minecraft.network.chat.Component;
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

    @Shadow
    @Final
    private static EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME;

    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE;

    @Unique
    private byte txoptimizations$sharedFlags;

    @Unique
    private Component txoptimizations$customName;

    @Unique
    private boolean txoptimizations$customNameVisible;

    @Override
    public void txoptimizations$storeSyncedValue(EntityDataAccessor<?> accessor, Object value) {
        if (accessor == DATA_SHARED_FLAGS_ID)
            this.txoptimizations$sharedFlags = (Byte) value;
        else if (accessor == DATA_CUSTOM_NAME)
            this.txoptimizations$customName = (Component) ((Optional<?>) value).orElse(null);
        else if (accessor == DATA_CUSTOM_NAME_VISIBLE)
            this.txoptimizations$customNameVisible = (Boolean) value;
    }

    @Overwrite
    protected boolean getSharedFlag(int flag) {
        return (this.txoptimizations$sharedFlags & 1 << flag) != 0;
    }

    @Overwrite
    public Component getCustomName() {
        return this.txoptimizations$customName;
    }

    @Overwrite
    public boolean hasCustomName() {
        return this.txoptimizations$customName != null;
    }

    @Overwrite
    public boolean isCustomNameVisible() {
        return this.txoptimizations$customNameVisible;
    }
}
