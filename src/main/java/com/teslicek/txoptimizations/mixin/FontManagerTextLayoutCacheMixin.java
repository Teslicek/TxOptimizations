package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontManager.class)
public abstract class FontManagerTextLayoutCacheMixin {

    @Inject(method = {"apply", "updateOptions"}, at = @At("TAIL"))
    private void txoptimizations$dropTextLayouts(CallbackInfo ci) {
        TextLayoutCache.clear();
    }
}
