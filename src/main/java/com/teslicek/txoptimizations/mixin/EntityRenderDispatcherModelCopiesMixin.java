package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.ModelCopies;
import java.util.List;
import java.util.Map;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherModelCopiesMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Shadow
    private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers;

    @Shadow
    private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers;

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void txoptimizations$copyModels(ResourceManager resourceManager, CallbackInfo ci, @Local EntityRendererProvider.Context context) {
        List<Object> originals = List.of(this.renderers, this.playerRenderers, this.mannequinRenderers);
        List<Object> copies    = List.of(EntityRenderers.createEntityRenderers(context), EntityRenderers.<AbstractClientPlayer>createAvatarRenderers(context), EntityRenderers.<ClientMannequin>createAvatarRenderers(context));

        ModelCopies.replace(ModelCopies.Source.ENTITIES, originals, copies);
    }
}
