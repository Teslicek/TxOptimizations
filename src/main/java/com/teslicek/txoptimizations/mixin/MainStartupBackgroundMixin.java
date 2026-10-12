package com.teslicek.txoptimizations.mixin;

import com.mojang.datafixers.DSL;
import com.teslicek.txoptimizations.StartupBackground;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Main.class)
public abstract class MainStartupBackgroundMixin {

    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/datafix/DataFixers;optimize(Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<?> txoptimizations$optimizeDataFixersInBackground(Set<DSL.TypeReference> types) {
        return StartupBackground.optimizeDataFixers(types);
    }

    @Redirect(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;preload()V"))
    private static void txoptimizations$preloadCrashReportInBackground() {
        StartupBackground.preloadCrashReport();
    }
}
