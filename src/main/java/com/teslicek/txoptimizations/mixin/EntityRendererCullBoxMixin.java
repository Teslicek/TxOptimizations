package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CullBoxCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullBoxMixin<T extends Entity> {

    @Redirect(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"))
    private AABB txoptimizations$reuseCullBox(AABB base, double amount, T entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return ((CullBoxCache) entity).txoptimizations$inflateCullBox(base, amount);
    }
}
