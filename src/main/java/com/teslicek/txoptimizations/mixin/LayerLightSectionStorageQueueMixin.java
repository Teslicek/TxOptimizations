package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageQueueMixin {

    @Shadow
    @Final
    protected Long2ObjectMap<DataLayer> queuedSections;

    @Unique
    private Long2ObjectOpenHashMap<DataLayer> txoptimizations$queuedTable;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMaps;synchronize(Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;)Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;"))
    private Long2ObjectMap<DataLayer> txoptimizations$keepQueuedTable(Long2ObjectMap<DataLayer> sections) {
        this.txoptimizations$queuedTable = (Long2ObjectOpenHashMap<DataLayer>) sections;

        return Long2ObjectMaps.synchronize(sections);
    }

    @Redirect(method = "markNewInconsistencies", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMaps;fastIterator(Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;)Lit/unimi/dsi/fastutil/objects/ObjectIterator;"))
    private ObjectIterator<Long2ObjectMap.Entry<DataLayer>> txoptimizations$queuedIterator(Long2ObjectMap<DataLayer> sections) {
        if (sections != this.queuedSections)
            throw new IllegalStateException("Unexpected light section map " + sections.getClass().getName());

        return this.txoptimizations$queuedTable.long2ObjectEntrySet().fastIterator();
    }
}
