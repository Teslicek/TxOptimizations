package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.GlyphProvider;
import com.teslicek.txoptimizations.UnifontCache;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(UnihexProvider.Definition.class)
public abstract class UnihexDefinitionCacheMixin {

    @Shadow
    @Final
    private Identifier hexFile;

    @Shadow
    @Final
    private List<?> sizeOverrides;

    @Shadow
    protected abstract UnihexProvider loadData(InputStream zipFile) throws IOException;

    @WrapMethod(method = "load")
    private GlyphProvider txoptimizations$reuseParsedUnifont(ResourceManager resourceManager, Operation<GlyphProvider> original) throws IOException {
        byte[] contents;

        try (InputStream raw = resourceManager.open(this.hexFile)) {
            contents = raw.readAllBytes();
        }

        byte[]        digest = UnifontCache.digest(contents);
        GlyphProvider cached = UnifontCache.find(this.hexFile, this.sizeOverrides, digest);

        if (cached != null)
            return cached;

        GlyphProvider provider = this.loadData(new ByteArrayInputStream(contents));

        UnifontCache.remember(this.hexFile, this.sizeOverrides, digest, provider);

        return provider;
    }
}
