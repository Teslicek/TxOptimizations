package com.teslicek.txoptimizations.mixin;

import net.minecraft.world.level.chunk.DataLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataLayer.class)
public abstract class DataLayerUniformMixin {

    @Shadow
    protected byte[] data;

    @Shadow
    private int defaultValue;

    @Inject(method = "<init>([B)V", at = @At("RETURN"))
    private void txoptimizations$dropUniformArray(byte[] data, CallbackInfo ci) {
        byte first = data[0];
        int  value = first & 15;

        if (value == 0 || (first >> 4 & 15) != value)
            return;

        for (byte packed : data) {
            if (packed != first)
                return;
        }

        this.data         = null;
        this.defaultValue = value;
    }
}
