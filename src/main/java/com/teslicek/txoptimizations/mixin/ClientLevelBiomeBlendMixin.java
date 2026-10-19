package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BiomeBlendWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 999)
public abstract class ClientLevelBiomeBlendMixin {

    @Overwrite
    public int calculateBlockTint(final BlockPos pos, final ColorResolver colorResolver) {
        ClientLevel level  = (ClientLevel) (Object) this;
        int         radius = Minecraft.getInstance().options.biomeBlendRadius().get();

        if (radius == 0)
            return colorResolver.getColor(level.getBiome(pos).value(), pos.getX(), pos.getZ());

        return BiomeBlendWindow.blend(level, pos, radius, colorResolver);
    }

    @Inject(method = "onChunkLoaded(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("HEAD"))
    private void txoptimizations$forgetBlendBiomesOnChunk(ChunkPos pos, CallbackInfo ci) {
        BiomeBlendWindow.biomesChanged();
    }

    @Inject(method = "clearTintCaches()V", at = @At("HEAD"))
    private void txoptimizations$forgetBlendBiomesOnClear(CallbackInfo ci) {
        BiomeBlendWindow.biomesChanged();
    }
}
