package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.GpuCubeBatch;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.GpuCubes;
import java.util.Arrays;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(StagedVertexBuffer.Draw.class)
public abstract class StagedVertexBufferDrawGpuCubesMixin implements GpuCubeDraw {

    @Unique
    private static final int UNKNOWN = 0;

    @Unique
    private static final int ACCEPTS = 1;

    @Unique
    private static final int REFUSES = 2;

    @Unique
    private static final int VALUES = 3;

    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    @Final
    private PrimitiveTopology primitiveTopology;

    @Shadow
    @Final
    private VertexSorting quadSorting;

    @Shadow
    private int vertexCount;

    @Unique
    private @Nullable GpuCubeBatch txoptimizations$batch;

    @Unique
    private @Nullable PreparedRenderType txoptimizations$prepared;

    @Unique
    private int txoptimizations$accepts;

    @Unique
    private int[] txoptimizations$segments = new int[0];

    @Unique
    private int txoptimizations$segmentCount;

    @Unique
    private int txoptimizations$rawMark;

    @Unique
    private int txoptimizations$nextRef = -1;

    @Unique
    private boolean txoptimizations$hasCubes;

    @Unique
    private boolean txoptimizations$finished;

    @Override
    public void txoptimizations$setBatch(GpuCubeBatch batch) {
        this.txoptimizations$batch = batch;
    }

    @Override
    public void txoptimizations$setPrepared(PreparedRenderType prepared) {
        if (this.txoptimizations$prepared != null && this.txoptimizations$prepared != prepared)
            throw new IllegalStateException("A staged draw was given a second render type " + prepared.name() + " after " + this.txoptimizations$prepared.name());

        this.txoptimizations$prepared = prepared;
    }

    @Override
    public boolean txoptimizations$acceptsCubes() {
        if (this.txoptimizations$accepts == UNKNOWN)
            this.txoptimizations$accepts = this.txoptimizations$batch != null && GpuCubes.accepts(this.format, this.primitiveTopology, this.quadSorting, this.txoptimizations$prepared) ? ACCEPTS : REFUSES;

        return this.txoptimizations$accepts == ACCEPTS;
    }

    @Override
    public GpuCubeBatch txoptimizations$batch() {
        GpuCubeBatch batch = this.txoptimizations$batch;

        if (batch == null)
            throw new IllegalStateException("A staged draw has no GPU cube batch");

        return batch;
    }

    @Override
    public int txoptimizations$rawVertices() {
        return this.vertexCount;
    }

    @Override
    public void txoptimizations$addCube(int ref, int rawVertices) {
        if (this.txoptimizations$finished)
            throw new IllegalStateException("A cube was added to a staged draw after it was drawn");

        if (rawVertices != this.txoptimizations$rawMark) {
            this.txoptimizations$push(false, this.txoptimizations$rawMark, rawVertices - this.txoptimizations$rawMark);
            this.txoptimizations$rawMark = rawVertices;
            this.txoptimizations$nextRef = -1;
        }

        if (ref == this.txoptimizations$nextRef) {
            int at = (this.txoptimizations$segmentCount - 1) * VALUES + 2;

            this.txoptimizations$segments[at] ++;
            this.txoptimizations$batch().noteRun(this.txoptimizations$segments[at]);
        } else {
            this.txoptimizations$push(true, ref, 1);
            this.txoptimizations$batch().noteRun(1);
        }

        this.txoptimizations$nextRef  = ref + 1;
        this.txoptimizations$hasCubes = true;
    }

    @Override
    public boolean txoptimizations$finishSegments() {
        if (this.txoptimizations$finished || !this.txoptimizations$hasCubes)
            return this.txoptimizations$hasCubes;

        if (this.vertexCount < this.txoptimizations$rawMark)
            throw new IllegalStateException("A staged draw has " + this.vertexCount + " vertices, fewer than the " + this.txoptimizations$rawMark + " counted before its last cube");

        if (this.vertexCount > this.txoptimizations$rawMark)
            this.txoptimizations$push(false, this.txoptimizations$rawMark, this.vertexCount - this.txoptimizations$rawMark);

        this.txoptimizations$finished = true;

        return true;
    }

    @Override
    public int txoptimizations$segments() {
        return this.txoptimizations$segmentCount;
    }

    @Override
    public boolean txoptimizations$isCubeSegment(int segment) {
        return this.txoptimizations$segments[segment * VALUES] != 0;
    }

    @Override
    public int txoptimizations$segmentStart(int segment) {
        return this.txoptimizations$segments[segment * VALUES + 1];
    }

    @Override
    public int txoptimizations$segmentLength(int segment) {
        return this.txoptimizations$segments[segment * VALUES + 2];
    }

    @Unique
    private void txoptimizations$push(boolean cubes, int start, int length) {
        int at = this.txoptimizations$segmentCount * VALUES;

        if (at + VALUES > this.txoptimizations$segments.length)
            this.txoptimizations$segments = Arrays.copyOf(this.txoptimizations$segments, Math.max(VALUES * 4, this.txoptimizations$segments.length * 2));

        this.txoptimizations$segments[at]     = cubes ? 1 : 0;
        this.txoptimizations$segments[at + 1] = start;
        this.txoptimizations$segments[at + 2] = length;
        this.txoptimizations$segmentCount ++;
    }
}
