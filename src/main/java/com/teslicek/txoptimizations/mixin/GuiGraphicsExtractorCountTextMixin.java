package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CountText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorCountTextMixin {

    @Redirect(method = "itemCount", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"), require = 1)
    private String txoptimizations$sharedCount(int count) {
        return CountText.of(count);
    }
}
