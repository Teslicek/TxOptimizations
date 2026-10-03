package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SpriteMarkVersion;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRegion.class, remap = false)
public abstract class RenderRegionSpriteMarkMixin {

    @Inject(method = {"setSectionRenderState", "clearSectionRenderState"}, at = @At("TAIL"))
    private void txoptimizations$invalidateSpriteMarks(CallbackInfo ci) {
        SpriteMarkVersion.bump();
    }
}
