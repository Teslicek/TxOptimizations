package com.teslicek.txoptimizations.mixin;

import java.util.Locale;
import net.minecraft.SystemReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import oshi.SystemInfo;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

@Mixin(SystemReport.class)
public abstract class SystemReportBrokenCountersMixin {

    @Unique
    private static final String SKIPPED = "Skipped: Windows performance counters for this are broken on this PC";

    @Shadow
    public abstract void setDetail(String key, String value);

    @Overwrite
    private void putSoftware(SystemInfo systemInfo) {
        OperatingSystem os = systemInfo.getOperatingSystem();

        this.setDetail("Operating System Version", os.toString());
        this.setDetail("Process Elevated", String.valueOf(os.isElevated()));
        this.setDetail("Process Loads", SKIPPED);
        this.setDetail("Process Virtual Size (MiB)", SKIPPED);
        this.setDetail("Process Resident Size (MiB)", SKIPPED);
    }

    @Overwrite
    private void putVirtualMemory(VirtualMemory virtualMemory) {
        this.setDetail("Virtual memory max (MiB)", mebibytes(virtualMemory.getVirtualMax()));
        this.setDetail("Virtual memory used (MiB)", mebibytes(virtualMemory.getVirtualInUse()));
        this.setDetail("Swap memory total (MiB)", mebibytes(virtualMemory.getSwapTotal()));
        this.setDetail("Swap memory used (MiB)", SKIPPED);
    }

    @Unique
    private static String mebibytes(long bytes) {
        return String.format(Locale.ROOT, "%.2f", SystemReport.sizeInMiB(bytes));
    }
}
