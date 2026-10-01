package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.AtlasFeatures;
import com.teslicek.txoptimizations.MapAtlas;
import com.teslicek.txoptimizations.MapAtlasSource;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapTextureManager.class)
public abstract class MapTextureManagerAtlasMixin implements MapAtlasSource {

    @Unique
    private final List<MapAtlas> txoptimizations$atlases = new ArrayList<>();

    @Unique
    private final Int2IntMap txoptimizations$locations = new Int2IntOpenHashMap();

    @Inject(method = "resetData", at = @At("RETURN"))
    private void txoptimizations$resetAtlases(CallbackInfo ci) {
        this.txoptimizations$atlases.forEach(MapAtlas::close);
        this.txoptimizations$atlases.clear();
        this.txoptimizations$locations.clear();
    }

    @Inject(method = "getOrCreateMapInstance", at = @At("HEAD"))
    private void txoptimizations$assignLocation(MapId id, MapItemSavedData data, CallbackInfoReturnable<?> cir) {
        if (!AtlasFeatures.isMapAtlasGeneration() || this.txoptimizations$locations.containsKey(id.id()))
            return;

        this.txoptimizations$locations.put(id.id(), this.txoptimizations$atlasWithSpace().allocate());
    }

    @Override
    public MapAtlas txoptimizations$getMapAtlas(int location) {
        return this.txoptimizations$atlases.get(MapAtlas.atlasId(location));
    }

    @Override
    public int txoptimizations$getMapAtlasLocation(int mapId) {
        return this.txoptimizations$locations.getOrDefault(mapId, NO_LOCATION);
    }

    @Unique
    private MapAtlas txoptimizations$atlasWithSpace() {
        if (!this.txoptimizations$atlases.isEmpty() && !this.txoptimizations$atlases.getLast().isFull())
            return this.txoptimizations$atlases.getLast();

        MapAtlas atlas = new MapAtlas(this.txoptimizations$atlases.size());
        this.txoptimizations$atlases.add(atlas);

        return atlas;
    }
}
