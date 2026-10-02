package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NameTagAttributeCache;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNameTagAttributeMixin implements NameTagAttributeCache {

    @Unique
    private AttributeMap txoptimizations$nameTagAttributeSource;

    @Unique
    private AttributeInstance txoptimizations$nameTagDistance;

    @Unique
    private AttributeInstance txoptimizations$belowNameDistance;

    @Override
    public AttributeMap txoptimizations$getNameTagAttributeSource() {
        return this.txoptimizations$nameTagAttributeSource;
    }

    @Override
    public AttributeInstance txoptimizations$getNameTagDistance() {
        return this.txoptimizations$nameTagDistance;
    }

    @Override
    public AttributeInstance txoptimizations$getBelowNameDistance() {
        return this.txoptimizations$belowNameDistance;
    }

    @Override
    public void txoptimizations$setNameTagAttributes(AttributeMap source, AttributeInstance nameTagDistance, AttributeInstance belowNameDistance) {
        this.txoptimizations$nameTagAttributeSource = source;
        this.txoptimizations$nameTagDistance        = nameTagDistance;
        this.txoptimizations$belowNameDistance      = belowNameDistance;
    }
}
