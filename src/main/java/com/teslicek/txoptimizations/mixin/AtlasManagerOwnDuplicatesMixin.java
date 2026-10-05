package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.teslicek.txoptimizations.BlocksAtlasSprites;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtlasManager.class)
public abstract class AtlasManagerOwnDuplicatesMixin {

    @WrapWithCondition(method = "lambda$updateSpriteMaps$0", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private static boolean txoptimizations$skipBakedSpriteDuplicates(Logger logger, String message, Object[] arguments) {
        return !BlocksAtlasSprites.isAddedDuplicate((Identifier) arguments[0], (Identifier) arguments[1], (Identifier) arguments[2]);
    }
}
