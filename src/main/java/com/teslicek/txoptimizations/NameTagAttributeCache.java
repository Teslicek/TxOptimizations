package com.teslicek.txoptimizations;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public interface NameTagAttributeCache {

    AttributeMap txoptimizations$getNameTagAttributeSource();

    AttributeInstance txoptimizations$getNameTagDistance();

    AttributeInstance txoptimizations$getBelowNameDistance();

    void txoptimizations$setNameTagAttributes(AttributeMap source, AttributeInstance nameTagDistance, AttributeInstance belowNameDistance);
}
