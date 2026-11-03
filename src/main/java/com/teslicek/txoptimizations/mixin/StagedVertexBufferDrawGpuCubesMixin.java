package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.teslicek.txoptimizations.GpuCubeBatch;
import com.teslicek.txoptimizations.GpuCubeDraw;
import com.teslicek.txoptimizations.GpuCubes;
import com.teslicek.txoptimizations.SkinAlpha;
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

    @Unique
    private static final int FACES = 6;

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
    private @Nullable SkinAlpha txoptimizations$skin;

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
    private int txoptimizations$nextKind;

    @Unique
    private boolean txoptimizations$hasCubes;

    @Unique
    private boolean txoptimizations$finished;

    @Unique
    private int[] txoptimizations$sortedRefs = new int[0];

    @Unique
    private int[] txoptimizations$sortedKeys = new int[0];

    @Unique
    private byte[] txoptimizations$sortedHiddenFaces = new byte[0];

    @Unique
    private int txoptimizations$sortedCubes;

    @Unique
    private int txoptimizations$sortedMode;

    @Unique
    private int txoptimizations$firstIndex;

    @Unique
    private int txoptimizations$indexCount;

    @Unique
    private long txoptimizations$vertexOffset;

    @Unique
    private long txoptimizations$vertexBytes;

    @Override
    public void txoptimizations$setBatch(GpuCubeBatch batch) {
        this.txoptimizations$batch = batch;
    }

    @Override
    public void txoptimizations$setPrepared(PreparedRenderType prepared) {
        if (this.txoptimizations$prepared != null && this.txoptimizations$prepared != prepared)
            throw new IllegalStateException("A staged draw was given a second render type " + prepared.name() + " after " + this.txoptimizations$prepared.name());

        if (this.txoptimizations$prepared == prepared)
            return;

        this.txoptimizations$prepared = prepared;
        this.txoptimizations$skin     = SkinAlpha.of(prepared);
    }

    @Override
    public @Nullable SkinAlpha txoptimizations$skin() {
        return this.txoptimizations$skin;
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
    public void txoptimizations$addRefs(int kind, int firstRef, int count, int rawVertices) {
        if (this.txoptimizations$finished)
            throw new IllegalStateException("A GPU shape was added to a staged draw after it was drawn");

        if (kind != CUBES && kind != QUADS)
            throw new IllegalArgumentException("Unknown GPU shape kind " + kind);

        if (rawVertices != this.txoptimizations$rawMark) {
            this.txoptimizations$push(RAW, this.txoptimizations$rawMark, rawVertices - this.txoptimizations$rawMark);
            this.txoptimizations$rawMark = rawVertices;
            this.txoptimizations$nextRef = -1;
        }

        int run;

        if (firstRef == this.txoptimizations$nextRef && kind == this.txoptimizations$nextKind) {
            int at = (this.txoptimizations$segmentCount - 1) * VALUES + 2;

            run = this.txoptimizations$segments[at] += count;
        } else {
            this.txoptimizations$push(kind, firstRef, count);
            run = count;
        }

        this.txoptimizations$batch().noteRun(run * (kind == CUBES ? GpuCubes.CUBE_INDICES : GpuCubes.QUAD_INDICES));
        this.txoptimizations$nextRef  = firstRef + count;
        this.txoptimizations$nextKind = kind;
        this.txoptimizations$hasCubes = true;
    }

    @Override
    public boolean txoptimizations$finishSegments() {
        if (this.txoptimizations$finished || !this.txoptimizations$hasCubes)
            return this.txoptimizations$hasCubes;

        if (this.vertexCount < this.txoptimizations$rawMark)
            throw new IllegalStateException("A staged draw has " + this.vertexCount + " vertices, fewer than the " + this.txoptimizations$rawMark + " counted before its last cube");

        if (this.vertexCount > this.txoptimizations$rawMark)
            this.txoptimizations$push(RAW, this.txoptimizations$rawMark, this.vertexCount - this.txoptimizations$rawMark);

        this.txoptimizations$finished = true;

        return true;
    }

    @Override
    public int txoptimizations$segments() {
        return this.txoptimizations$segmentCount;
    }

    @Override
    public int txoptimizations$segmentKind(int segment) {
        return this.txoptimizations$segments[segment * VALUES];
    }

    @Override
    public int txoptimizations$segmentStart(int segment) {
        return this.txoptimizations$segments[segment * VALUES + 1];
    }

    @Override
    public int txoptimizations$segmentLength(int segment) {
        return this.txoptimizations$segments[segment * VALUES + 2];
    }

    @Override
    public boolean txoptimizations$isSorted() {
        return this.quadSorting != null;
    }

    @Override
    public void txoptimizations$addSortedKeys(int ref, int[] keys, int hiddenFaces) {
        if ((hiddenFaces & ~((1 << FACES) - 1)) != 0)
            throw new IllegalArgumentException("A sorted GPU cube has hidden faces " + Integer.toBinaryString(hiddenFaces) + " beyond its " + FACES + " faces");

        int cube = this.txoptimizations$sortedCubes;

        if (cube == this.txoptimizations$sortedRefs.length) {
            this.txoptimizations$sortedRefs        = Arrays.copyOf(this.txoptimizations$sortedRefs, Math.max(16, cube * 2));
            this.txoptimizations$sortedKeys        = Arrays.copyOf(this.txoptimizations$sortedKeys, this.txoptimizations$sortedRefs.length * FACES);
            this.txoptimizations$sortedHiddenFaces = Arrays.copyOf(this.txoptimizations$sortedHiddenFaces, this.txoptimizations$sortedRefs.length);
        }

        if (cube == 0)
            this.txoptimizations$batch().addSortedDraw(this);

        this.txoptimizations$sortedRefs[cube]        = ref;
        this.txoptimizations$sortedHiddenFaces[cube] = (byte) hiddenFaces;
        System.arraycopy(keys, 0, this.txoptimizations$sortedKeys, cube * FACES, FACES);
        this.txoptimizations$sortedCubes ++;
    }

    @Override
    public int txoptimizations$sortedCubes() {
        return this.txoptimizations$sortedCubes;
    }

    @Override
    public int[] txoptimizations$sortedRefs() {
        return this.txoptimizations$sortedRefs;
    }

    @Override
    public int[] txoptimizations$sortedKeys() {
        return this.txoptimizations$sortedKeys;
    }

    @Override
    public byte[] txoptimizations$sortedHiddenFaces() {
        return this.txoptimizations$sortedHiddenFaces;
    }

    @Override
    public void txoptimizations$setSortedDraw(boolean expanded, int firstIndex, int indexCount, long vertexOffset, long vertexBytes) {
        this.txoptimizations$sortedMode   = expanded ? 2 : 1;
        this.txoptimizations$firstIndex   = firstIndex;
        this.txoptimizations$indexCount   = indexCount;
        this.txoptimizations$vertexOffset = vertexOffset;
        this.txoptimizations$vertexBytes  = vertexBytes;
    }

    @Override
    public int txoptimizations$sortedMode() {
        return this.txoptimizations$sortedMode;
    }

    @Override
    public int txoptimizations$firstIndex() {
        return this.txoptimizations$firstIndex;
    }

    @Override
    public int txoptimizations$indexCount() {
        return this.txoptimizations$indexCount;
    }

    @Override
    public long txoptimizations$vertexOffset() {
        return this.txoptimizations$vertexOffset;
    }

    @Override
    public long txoptimizations$vertexBytes() {
        return this.txoptimizations$vertexBytes;
    }

    @Unique
    private void txoptimizations$push(int kind, int start, int length) {
        int at = this.txoptimizations$segmentCount * VALUES;

        if (at + VALUES > this.txoptimizations$segments.length)
            this.txoptimizations$segments = Arrays.copyOf(this.txoptimizations$segments, Math.max(VALUES * 4, this.txoptimizations$segments.length * 2));

        this.txoptimizations$segments[at]     = kind;
        this.txoptimizations$segments[at + 1] = start;
        this.txoptimizations$segments[at + 2] = length;
        this.txoptimizations$segmentCount ++;
    }
}
