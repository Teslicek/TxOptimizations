package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CullBoxCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityCullBoxCacheMixin implements CullBoxCache {

    @Unique
    private AABB txoptimizations$cullBase;

    @Unique
    private double txoptimizations$cullAmount;

    @Unique
    private AABB txoptimizations$cullBox;

    @Override
    public AABB txoptimizations$inflateCullBox(AABB base, double amount) {
        if (base != this.txoptimizations$cullBase || amount != this.txoptimizations$cullAmount) {
            this.txoptimizations$cullBox    = base.inflate(amount);
            this.txoptimizations$cullBase   = base;
            this.txoptimizations$cullAmount = amount;
        }

        return this.txoptimizations$cullBox;
    }
}
