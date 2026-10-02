package com.teslicek.txoptimizations.mixin.cull;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.cull.Cullable;
import com.teslicek.txoptimizations.cull.EntityCulling;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorCullMixin {

    @Shadow
    private EntityRenderState extractEntity(Entity entity, float partialTickTime) {
        throw new AssertionError();
    }

    @ModifyExpressionValue(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;isEntityVisible(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDDFJ)Z"))
    private boolean txoptimizations$skipCulledEntities(boolean visible, @Local Entity entity) {
        return visible && !EntityCulling.hidesEntity(entity);
    }

    @Redirect(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState txoptimizations$extractCulledNameTag(LevelExtractor extractor, Entity entity, float partialTick) {
        Cullable cullable = (Cullable) entity;

        if (cullable.txoptimizations$isCulled())
            return EntityCulling.nameTagState(entity, partialTick);

        cullable.txoptimizations$setOutOfCamera(false);

        return this.extractEntity(entity, partialTick);
    }
}
