package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.EntityStateReuse;
import com.teslicek.txoptimizations.LeashHolder;
import com.teslicek.txoptimizations.NameTagMemo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;getLeashHolder()Lnet/minecraft/world/entity/Entity;"))
    private Entity txoptimizations$leashHolder(Leashable leashable) {
        return LeashHolder.of(leashable);
    }

    @ModifyExpressionValue(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;"))
    private Component txoptimizations$reuseNameTag(Component nameTag, @Local(argsOnly = true) Entity entity) {
        return ((NameTagMemo) entity).txoptimizations$reuseNameTag(nameTag);
    }

    @ModifyExpressionValue(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;belowNameDisplay()Lnet/minecraft/network/chat/Component;"))
    private Component txoptimizations$reuseScoreText(Component scoreText, @Local(argsOnly = true) Entity entity) {
        return ((NameTagMemo) entity).txoptimizations$reuseScoreText(scoreText);
    }

    @Redirect(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityAttachments;getNullable(Lnet/minecraft/world/entity/EntityAttachment;IF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 txoptimizations$reuseNameTagAttachment(EntityAttachments attachments, EntityAttachment attachment, int index, float yRot, Entity entity, EntityRenderState state, float partialTicks, double nameTagDistance, double belowNameDistance) {
        return ((NameTagMemo) entity).txoptimizations$nameTagAttachment(attachments, yRot);
    }

    @Redirect(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState()Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState txoptimizations$reuseWorldState(EntityRenderer<?, ?> renderer, Entity entity, float partialTicks) {
        return EntityStateReuse.stateFor(renderer, entity);
    }
}
