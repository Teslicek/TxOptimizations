package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AvatarModelLookup;
import com.teslicek.txoptimizations.AvatarSkins;
import java.util.Map;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherSkinCacheMixin {

    @Unique
    private volatile AvatarModelLookup txoptimizations$firstLookup;

    @Unique
    private volatile AvatarModelLookup txoptimizations$secondLookup;

    @Redirect(method = "getAvatarRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarEntity;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;"))
    private PlayerSkin txoptimizations$reuseFrameSkin(ClientAvatarEntity avatar) {
        return AvatarSkins.frameSkin(avatar);
    }

    @Redirect(method = "getAvatarRenderer", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object txoptimizations$rendererForModel(Map<?, ?> renderers, Object model) {
        AvatarModelLookup first = this.txoptimizations$firstLookup;

        if (first != null && first.source() == renderers)
            return first.renderer((PlayerModelType) model);

        AvatarModelLookup second = this.txoptimizations$secondLookup;

        if (second != null && second.source() == renderers)
            return second.renderer((PlayerModelType) model);

        AvatarModelLookup lookup = AvatarModelLookup.of(renderers);

        this.txoptimizations$secondLookup = first;
        this.txoptimizations$firstLookup  = lookup;

        return lookup.renderer((PlayerModelType) model);
    }
}
