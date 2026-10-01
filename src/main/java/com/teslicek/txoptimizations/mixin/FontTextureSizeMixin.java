package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AtlasFeatures;
import net.minecraft.client.gui.font.FontTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontTexture.class)
public abstract class FontTextureSizeMixin {

    @Unique
    private static final int VANILLA_SIZE = 256;

    @Unique
    private int txoptimizations$size;

    @Inject(method = "<init>", at = @At("CTOR_HEAD"))
    private void txoptimizations$chooseSize(CallbackInfo ci) {
        this.txoptimizations$size = AtlasFeatures.isFontAtlasResizing() ? AtlasFeatures.FONT_ATLAS_SIZE : VANILLA_SIZE;
    }

    @ModifyConstant(method = "*", constant = @Constant(intValue = VANILLA_SIZE))
    private int txoptimizations$resizeInt(int original) {
        return this.txoptimizations$size;
    }

    @ModifyConstant(method = "*", constant = @Constant(floatValue = VANILLA_SIZE))
    private float txoptimizations$resizeFloat(float original) {
        return this.txoptimizations$size;
    }
}
