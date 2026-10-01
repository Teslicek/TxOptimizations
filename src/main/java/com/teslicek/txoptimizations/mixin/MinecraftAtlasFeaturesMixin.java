package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AtlasFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftAtlasFeaturesMixin {

    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Shadow
    @Final
    private FontManager fontManager;

    @Shadow
    @Final
    public Options options;

    @Shadow
    public abstract VanillaPackResources getVanillaPackResources();

    @Shadow
    public abstract MapTextureManager getMapTextureManager();

    @Inject(method = "onResourceLoadFinished", at = @At("RETURN"))
    private void txoptimizations$applyResourcePackAtlasFeatures(CallbackInfo ci) {
        boolean fontAtlasResizing  = AtlasFeatures.isFontAtlasResizing();
        boolean mapAtlasGeneration = AtlasFeatures.isMapAtlasGeneration();

        AtlasFeatures.updateFromResourcePacks(this.resourceManager, this.getVanillaPackResources().fullResources());

        if (fontAtlasResizing != AtlasFeatures.isFontAtlasResizing())
            this.fontManager.updateOptions(this.options);

        if (mapAtlasGeneration != AtlasFeatures.isMapAtlasGeneration())
            this.getMapTextureManager().resetData();
    }
}
