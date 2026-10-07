package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.FastFilePackResources;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FilePackResources.FileResourcesSupplier.class)
public abstract class FileResourcesSupplierMixin {

    @Shadow
    @Final
    private File content;

    @Inject(method = "openResources", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$useFastFilePackResources(PackLocationInfo packLocationInfo, Pack.Metadata metadata, CallbackInfoReturnable<Stream<PackResources>> cir) {
        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(this.content);
        } catch (IOException e) {
            FastFilePackResources.LOGGER.error("Failed to open pack {}", this.content, e);
        }

        cir.setReturnValue(Stream.of(new FastFilePackResources(packLocationInfo, zipFile, metadata.overlays())));
    }
}
