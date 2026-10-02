package com.teslicek.txoptimizations.mixin;

import java.util.Arrays;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderRegion.class, priority = 1100)
public abstract class RenderRegionPassArrayMixin {

    @Shadow
    @Final
    private Map<TerrainRenderPass, MultiDrawBatch> cachedBatches;

    @Unique
    private final SectionRenderDataStorage[] txoptimizations$storages = new SectionRenderDataStorage[DefaultTerrainRenderPasses.ALL.length];

    @Unique
    private final MultiDrawBatch[] txoptimizations$batches = new MultiDrawBatch[DefaultTerrainRenderPasses.ALL.length];

    @Overwrite
    public SectionRenderDataStorage getStorage(TerrainRenderPass pass) {
        return this.txoptimizations$storages[DefaultTerrainRenderPasses.getPassIndex(pass)];
    }

    @Inject(method = "createStorage", at = @At("RETURN"))
    private void txoptimizations$rememberStorage(TerrainRenderPass pass, CallbackInfoReturnable<SectionRenderDataStorage> cir) {
        this.txoptimizations$storages[DefaultTerrainRenderPasses.getPassIndex(pass)] = cir.getReturnValue();
    }

    @Overwrite
    public MultiDrawBatch getCachedBatch(TerrainRenderPass pass) {
        int            index = DefaultTerrainRenderPasses.getPassIndex(pass);
        MultiDrawBatch batch = this.txoptimizations$batches[index];

        if (batch != null)
            return batch;

        batch = MultiDrawBatch.newBatch(ModelQuadFacing.COUNT * 256 + 1);
        this.cachedBatches.put(pass, batch);
        this.txoptimizations$batches[index] = batch;

        return batch;
    }

    @Overwrite
    public void clearCachedBatchFor(TerrainRenderPass pass) {
        MultiDrawBatch batch = this.txoptimizations$batches[DefaultTerrainRenderPasses.getPassIndex(pass)];

        if (batch != null)
            batch.clear();
    }

    @Overwrite
    public void clearAllCachedBatches() {
        for (MultiDrawBatch batch : this.txoptimizations$batches) {
            if (batch != null)
                batch.clear();
        }
    }

    @Inject(method = "delete", at = @At("TAIL"))
    private void txoptimizations$forgetPasses(CallbackInfo ci) {
        Arrays.fill(this.txoptimizations$storages, null);
        Arrays.fill(this.txoptimizations$batches, null);
    }
}
