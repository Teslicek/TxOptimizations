package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.NameTagAttributeCache;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererNameTagAttributeMixin {

    @WrapOperation(method = "extractNameTags", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttribute(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;"))
    private AttributeInstance txoptimizations$reuseNameTagAttribute(LivingEntity entity, Holder<Attribute> attribute, Operation<AttributeInstance> original) {
        NameTagAttributeCache cache      = (NameTagAttributeCache) entity;
        AttributeMap          attributes = entity.getAttributes();

        if (cache.txoptimizations$getNameTagAttributeSource() != attributes)
            cache.txoptimizations$setNameTagAttributes(attributes, original.call(entity, Attributes.NAME_TAG_DISTANCE), original.call(entity, Attributes.BELOW_NAME_DISTANCE));

        if (attribute == Attributes.NAME_TAG_DISTANCE)
            return cache.txoptimizations$getNameTagDistance();

        if (attribute == Attributes.BELOW_NAME_DISTANCE)
            return cache.txoptimizations$getBelowNameDistance();

        return original.call(entity, attribute);
    }
}
