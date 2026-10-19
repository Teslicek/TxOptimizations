package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.NameTagMemo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMemoMixin {

    @ModifyExpressionValue(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;"))
    private Component txoptimizations$reuseNameTag(Component nameTag, @Local(argsOnly = true) Entity entity) {
        return ((NameTagMemo) entity).txoptimizations$reuseNameTag(nameTag);
    }

    @ModifyExpressionValue(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;belowNameDisplay()Lnet/minecraft/network/chat/Component;"))
    private Component txoptimizations$reuseScoreText(Component scoreText, @Local(argsOnly = true) Entity entity) {
        return ((NameTagMemo) entity).txoptimizations$reuseScoreText(scoreText);
    }
}
