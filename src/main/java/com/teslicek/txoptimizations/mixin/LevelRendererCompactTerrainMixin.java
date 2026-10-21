package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.CompactTerrain;
import com.teslicek.txoptimizations.PackedTerrain;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCompactTerrainMixin {

    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;

    @Shadow
    public abstract void resetLevelRenderData();

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), require = 1)
    private void txoptimizations$matchTerrainFormat(ClientLevel level, Options options, Camera camera, BlockColors blockColors, CallbackInfo ci) {
        if (this.sectionRenderDispatcher != null && CompactTerrain.active() != PackedTerrain.isPackedSource())
            this.resetLevelRenderData();
    }
}
