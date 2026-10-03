package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.mods.sodium.client.gpu.arena.ArenaAggregator;
import net.caffeinemc.mods.sodium.client.gpu.arena.staging.StagingBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.IntPool;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRegionManager.class, remap = false)
public abstract class RenderRegionManagerListMixin {

    @Shadow
    @Final
    private Long2ReferenceOpenHashMap<RenderRegion> regions;

    @Shadow
    @Final
    private StagingBuffer stagingBuffer;

    @Shadow
    @Final
    private IntPool freeIds;

    @Shadow
    @Final
    private ArenaAggregator arenaAggregator;

    @Unique
    private final ReferenceArrayList<RenderRegion> txoptimizations$regionList = new ReferenceArrayList<>();

    @Overwrite
    public void update(UniformBufferManager uniforms) {
        this.stagingBuffer.flip();
        this.arenaAggregator.update();

        ReferenceArrayList<RenderRegion> list = this.txoptimizations$regionList;
        int                              index = 0;

        while (index < list.size()) {
            RenderRegion region = list.get(index);

            region.update();

            if (!region.isEmpty()) {
                index ++;
                continue;
            }

            region.delete();

            if (region.getId() != -1) {
                this.freeIds.release(region.getId());
                uniforms.clearRegionTimes(region.getId());
            }

            if (this.regions.remove(RenderRegion.key(region.getX(), region.getY(), region.getZ())) != region)
                throw new IllegalStateException("Render region list and map disagree");

            int last = list.size() - 1;

            list.set(index, list.get(last));
            list.remove(last);
        }
    }

    @Overwrite
    private RenderRegion create(int x, int y, int z) {
        long         key      = RenderRegion.key(x, y, z);
        RenderRegion instance = this.regions.get(key);

        if (instance == null) {
            instance = new RenderRegion(x, y, z, this.arenaAggregator);
            this.regions.put(key, instance);
            this.txoptimizations$regionList.add(instance);
        }

        return instance;
    }

    @Inject(method = "delete", at = @At("TAIL"))
    private void txoptimizations$forgetRegions(CallbackInfo ci) {
        this.txoptimizations$regionList.clear();
    }
}
