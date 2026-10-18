package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.EntityValues;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityLookup.class)
public abstract class EntityLookupValuesMixin<T extends EntityAccess> {

    @Shadow
    @Final
    private Int2ObjectMap<T> byId;

    @Overwrite
    public Iterable<T> getAllEntities() {
        return new EntityValues<>(this.byId.values());
    }
}
