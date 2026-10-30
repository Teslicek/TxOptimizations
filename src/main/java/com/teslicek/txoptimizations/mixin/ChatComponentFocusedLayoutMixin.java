package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.TextLayoutCache;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
public abstract class ChatComponentFocusedLayoutMixin {

    @ModifyVariable(method = "handleMessage", at = @At("HEAD"), argsOnly = true, require = 1)
    private FormattedCharSequence txoptimizations$holdChatLine(FormattedCharSequence message) {
        return TextLayoutCache.hold(message);
    }
}
