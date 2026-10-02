package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.teslicek.txoptimizations.MapAtlas;
import com.teslicek.txoptimizations.MapAtlasSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public abstract class MapInstanceAtlasMixin {

    @Shadow
    @Final
    private DynamicTexture texture;

    @Unique
    private GpuTexture txoptimizations$atlasTexture;

    @Unique
    private int txoptimizations$atlasX;

    @Unique
    private int txoptimizations$atlasY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$allocateAtlasSlot(MapTextureManager manager, int id, MapItemSavedData data, CallbackInfo ci) {
        MapAtlasSource source   = (MapAtlasSource) manager;
        int            location = source.txoptimizations$allocateMapAtlasLocation(id);

        if (location == MapAtlasSource.NO_LOCATION)
            return;

        this.txoptimizations$atlasTexture = source.txoptimizations$getMapAtlas(location).getGpuTexture();
        this.txoptimizations$atlasX       = MapAtlas.pixelX(location);
        this.txoptimizations$atlasY       = MapAtlas.pixelY(location);
    }

    @Inject(method = "updateTextureIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V", shift = At.Shift.AFTER))
    private void txoptimizations$copyIntoAtlas(CallbackInfo ci) {
        if (this.txoptimizations$atlasTexture == null)
            return;

        RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.txoptimizations$atlasTexture, this.texture.getPixels(), 0, 0, this.txoptimizations$atlasX, this.txoptimizations$atlasY);
    }
}
