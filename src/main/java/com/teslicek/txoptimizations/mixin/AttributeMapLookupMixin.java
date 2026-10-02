package com.teslicek.txoptimizations.mixin;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AttributeMap.class)
public abstract class AttributeMapLookupMixin {

    @Shadow
    @Final
    private Map<Holder<Attribute>, AttributeInstance> attributes;

    @Shadow
    @Final
    private AttributeSupplier supplier;

    @Shadow
    private void onAttributeModified(AttributeInstance instance) {
        throw new AssertionError();
    }

    @Overwrite
    public AttributeInstance getInstance(Holder<Attribute> attribute) {
        AttributeInstance instance = this.attributes.get(attribute);

        if (instance != null)
            return instance;

        return this.attributes.computeIfAbsent(attribute, key -> this.supplier.createInstance(this::onAttributeModified, key));
    }
}
