package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.PaletteReads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerReadsMixin implements PaletteReads {

    @Unique
    private int txoptimizations$reads;

    @Inject(method = "read", at = @At("HEAD"))
    private void txoptimizations$countRead(FriendlyByteBuf buffer, CallbackInfo ci) {
        this.txoptimizations$reads ++;
    }

    @Override
    public int txoptimizations$reads() {
        return this.txoptimizations$reads;
    }
}
