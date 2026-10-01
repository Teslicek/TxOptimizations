package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.MapAtlas;
import com.teslicek.txoptimizations.MapAtlasRenderState;
import com.teslicek.txoptimizations.MapAtlasSource;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public abstract class MapRendererAtlasMixin {

    @Shadow
    @Final
    private MapTextureManager mapTextureManager;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void txoptimizations$pointAtAtlas(MapId mapId, MapItemSavedData mapData, MapRenderState mapRenderState, CallbackInfo ci) {
        MapAtlasSource      source   = (MapAtlasSource) this.mapTextureManager;
        MapAtlasRenderState state    = (MapAtlasRenderState) mapRenderState;
        int                 location = source.txoptimizations$getMapAtlasLocation(mapId.id());

        if (location == MapAtlasSource.NO_LOCATION) {
            state.txoptimizations$setAtlas(null, 0.0F, 0.0F);

            return;
        }

        mapRenderState.texture = source.txoptimizations$getMapAtlas(location).getTextureId();
        state.txoptimizations$setAtlas(mapRenderState.texture, (float) MapAtlas.pixelX(location) / MapAtlas.ATLAS_SIZE, (float) MapAtlas.pixelY(location) / MapAtlas.ATLAS_SIZE);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V", ordinal = 0))
    private SubmitNodeCollector.CustomGeometryRenderer txoptimizations$drawFromAtlas(SubmitNodeCollector.CustomGeometryRenderer geometry, @Local(name = "mapRenderState", argsOnly = true) MapRenderState mapRenderState, @Local(name = "lightCoords", argsOnly = true) int lightCoords) {
        MapAtlasRenderState state = (MapAtlasRenderState) mapRenderState;

        if (state.txoptimizations$getAtlasTexture() == null || !state.txoptimizations$getAtlasTexture().equals(mapRenderState.texture))
            return geometry;

        float u0 = state.txoptimizations$getAtlasU();
        float v0 = state.txoptimizations$getAtlasV();
        float u1 = u0 + MapAtlas.MAP_UV;
        float v1 = v0 + MapAtlas.MAP_UV;

        return (pose, vertices) -> {
            vertices.addVertex(pose, 0.0F, MapAtlas.MAP_SIZE, -0.01F).setColor(-1).setUv(u0, v1).setLight(lightCoords);
            vertices.addVertex(pose, MapAtlas.MAP_SIZE, MapAtlas.MAP_SIZE, -0.01F).setColor(-1).setUv(u1, v1).setLight(lightCoords);
            vertices.addVertex(pose, MapAtlas.MAP_SIZE, 0.0F, -0.01F).setColor(-1).setUv(u1, v0).setLight(lightCoords);
            vertices.addVertex(pose, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(u0, v0).setLight(lightCoords);
        };
    }
}
