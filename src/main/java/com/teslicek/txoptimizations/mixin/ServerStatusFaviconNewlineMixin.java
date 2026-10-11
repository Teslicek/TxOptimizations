package com.teslicek.txoptimizations.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.network.protocol.status.ServerStatus$Favicon")
public abstract class ServerStatusFaviconNewlineMixin {

    @Redirect(method = "lambda$static$0", at = @At(value = "INVOKE", target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
    private static String txoptimizations$removeNewlines(String base64, String newline, String replacement) {
        if (!newline.equals("\n") || !replacement.isEmpty())
            throw new IllegalStateException("Unexpected favicon replacement " + newline + " -> " + replacement);

        if (base64.indexOf('\n') < 0)
            return base64;

        return base64.replace("\n", "");
    }
}
