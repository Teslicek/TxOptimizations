package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ModResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
public abstract class MinecraftModResourcesMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"))
    private RepositorySource[] txoptimizations$addModResources(RepositorySource[] sources) {
        return ModResourcePack.withSource(sources);
    }
}
