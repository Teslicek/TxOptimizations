package com.teslicek.txoptimizations.mixin;

import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public abstract class MapInstanceColorTableMixin {

    @Unique
    private static final int[] COLORS = txoptimizations$colorTable();

    @Redirect(method = "updateTextureIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/MapColor;getColorFromPackedId(I)I"))
    private int txoptimizations$lookUpColor(int packedId) {
        return COLORS[packedId & 0xFF];
    }

    @Unique
    private static int[] txoptimizations$colorTable() {
        int[] colors = new int[256];

        for (int packedId = 0; packedId < colors.length; packedId ++)
            colors[packedId] = MapColor.getColorFromPackedId(packedId);

        return colors;
    }
}
