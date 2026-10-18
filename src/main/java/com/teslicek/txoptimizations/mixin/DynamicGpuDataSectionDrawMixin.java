package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.teslicek.txoptimizations.SectionCommandStorage;
import com.teslicek.txoptimizations.SectionDrawList;
import com.teslicek.txoptimizations.SectionDrawUniforms;
import com.teslicek.txoptimizations.SectionInfoStorage;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.client.renderer.DynamicGpuDataStorageNonMapped;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DynamicGpuData.class)
public abstract class DynamicGpuDataSectionDrawMixin implements SectionDrawUniforms {

    @Shadow
    private @Nullable DynamicGpuDataStorage<DynamicGpuData.ChunkSectionInfo> chunkSections;

    @Shadow
    private @Nullable DynamicGpuDataStorageMapped<DynamicGpuData.IndexedDraw> chunkSectionsCommandBuffer;

    @Override
    public GpuBufferSlice[] txoptimizations$writeSectionCommands(SectionDrawList draws) {
        if (this.chunkSectionsCommandBuffer == null)
            this.chunkSectionsCommandBuffer = new DynamicGpuDataStorageMapped<>("Chunk Sections Command Buffer", SectionDrawList.COMMAND_BYTES, GpuBuffer.USAGE_INDIRECT_PARAMETERS, 2);

        return ((SectionCommandStorage) this.chunkSectionsCommandBuffer).txoptimizations$writeSectionCommands(draws);
    }

    @Override
    public GpuBufferSlice txoptimizations$writeSectionInfos(SectionDrawList draws) {
        if (this.chunkSections != null && (this.chunkSections.usage() & GpuBuffer.USAGE_VERTEX) == 0) {
            this.chunkSections.close();
            this.chunkSections = null;
        }

        if (this.chunkSections == null)
            this.chunkSections = new DynamicGpuDataStorageNonMapped<>("Chunk Sections Instanced", DynamicGpuData.CHUNK_SECTION_UBO_SIZE, GpuBuffer.USAGE_VERTEX, 2);

        if (!(this.chunkSections instanceof SectionInfoStorage storage))
            throw new IllegalStateException("Chunk section infos are stored in " + this.chunkSections + ", which cannot take raw section infos");

        return storage.txoptimizations$writeSectionInfos(draws);
    }
}
