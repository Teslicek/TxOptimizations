package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.DebugFormatCache;
import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {
    "net.minecraft.client.gui.components.debug.DebugEntryFps",
    "net.minecraft.client.gui.components.debug.DebugEntryLocalDifficulty",
    "net.minecraft.client.gui.components.debug.DebugEntryMemory",
    "net.minecraft.client.gui.components.debug.DebugEntryPlayerSpeed",
    "net.minecraft.client.gui.components.debug.DebugEntryPosition",
    "net.minecraft.client.gui.components.debug.DebugEntrySectionPosition",
    "net.minecraft.client.gui.components.debug.DebugEntrySimplePerformanceImpactors",
    "net.minecraft.client.gui.components.debug.DebugEntrySoundCache",
    "net.minecraft.client.gui.components.debug.DebugEntrySoundMood",
    "net.minecraft.client.gui.components.debug.DebugEntrySystemSpecs",
    "net.minecraft.client.gui.components.debug.DebugEntryTps"
})
public abstract class DebugEntryFormatCacheMixin {

    @Redirect(method = "display", at = @At(value = "INVOKE", target = "Ljava/lang/String;format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
    private String txoptimizations$cachedFormat(Locale locale, String format, Object[] args) {
        return DebugFormatCache.format(locale, format, args);
    }
}
