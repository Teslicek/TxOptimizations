package com.teslicek.txoptimizations.mixin;

import net.minecraft.SystemReport;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import oshi.software.os.OSProcess;

@Mixin(SystemReport.class)
public abstract class SystemReportMissingProcessMixin {

    @Unique
    private static final String UNAVAILABLE = "Unavailable: the operating system did not report this process";

    @Shadow
    public abstract void setDetail(String key, String value);

    @Inject(method = "putProcessDetails", at = @At("HEAD"), cancellable = true, require = 1)
    private void txoptimizations$reportMissingProcess(@Nullable OSProcess process, CallbackInfo ci) {
        if (process != null)
            return;

        this.setDetail("Process Loads", UNAVAILABLE);
        this.setDetail("Process Virtual Size (MiB)", UNAVAILABLE);
        this.setDetail("Process Resident Size (MiB)", UNAVAILABLE);
        ci.cancel();
    }
}
