package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionNameWidthMixin {

    @Redirect(method = "submitNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"))
    private int txoptimizations$cachedNameWidth(Font font, FormattedText text) {
        return TextLayoutCache.width(font, text);
    }
}
