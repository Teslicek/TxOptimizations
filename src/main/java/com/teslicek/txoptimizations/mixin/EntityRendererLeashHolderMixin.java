package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererLeashHolderMixin {

    @Unique
    private static final int NO_DELAYED_HOLDER = 0;

    @Redirect(method = {"shouldRender", "extractRenderState"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;getLeashHolder()Lnet/minecraft/world/entity/Entity;"))
    private Entity txoptimizations$leashHolder(Leashable leashable) {
        Leashable.LeashData leashData = leashable.getLeashData();

        if (leashData == null)
            return null;

        int delayedHolderId = ((LeashDataAccessor) (Object) leashData).txoptimizations$delayedLeashHolderId();

        if (delayedHolderId == NO_DELAYED_HOLDER)
            return leashData.leashHolder;

        Entity entity = (Entity) leashable;
        Entity holder = entity.level().getEntity(delayedHolderId);

        if (entity.level().isClientSide() && holder != null)
            leashData.setLeashHolder(holder);

        return leashData.leashHolder;
    }
}
