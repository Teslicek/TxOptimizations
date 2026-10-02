package com.teslicek.txoptimizations.mixin.cull;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererCullAccessor {

    @Invoker("affectedByCulling")
    boolean txoptimizations$affectedByCulling(Entity entity);

    @Invoker("getBoundingBoxForCulling")
    AABB txoptimizations$getBoundingBoxForCulling(Entity entity, float partialTicks);

    @Invoker("shouldShowName")
    boolean txoptimizations$shouldShowName(Entity entity, double distanceToCameraSq);
}
