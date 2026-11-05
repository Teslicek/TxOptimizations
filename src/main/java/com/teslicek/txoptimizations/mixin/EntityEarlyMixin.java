package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import com.teslicek.txoptimizations.SharedFlags;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Entity.class, priority = 999)
public abstract class EntityEarlyMixin implements SharedFlags {

    @Shadow
    private ImmutableList<Entity> passengers;

    @Shadow
    private Stream<Entity> getIndirectPassengersStream() {
        throw new AssertionError();
    }

    @Overwrite
    public int countPlayerPassengers() {
        if (this.passengers.isEmpty())
            return 0;

        return (int) this.getIndirectPassengersStream().filter(passenger -> passenger instanceof Player).count();
    }

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

    @Unique
    private Vec3 txoptimizations$viewVector;

    @Unique
    private int txoptimizations$viewXRot;

    @Unique
    private int txoptimizations$viewYRot;

    @Shadow
    public abstract float getViewXRot(float partialTicks);

    @Shadow
    public abstract float getViewYRot(float partialTicks);

    @Overwrite
    public final Vec3 getViewVector(float partialTicks) {
        float xRot  = this.getViewXRot(partialTicks);
        float yRot  = this.getViewYRot(partialTicks);
        int   xBits = Float.floatToRawIntBits(xRot);
        int   yBits = Float.floatToRawIntBits(yRot);
        Vec3  last  = this.txoptimizations$viewVector;

        if (last != null && xBits == this.txoptimizations$viewXRot && yBits == this.txoptimizations$viewYRot)
            return last;

        Vec3 vector = Entity.calculateViewVector(xRot, yRot);

        this.txoptimizations$viewVector = vector;
        this.txoptimizations$viewXRot   = xBits;
        this.txoptimizations$viewYRot   = yBits;

        return vector;
    }
}
