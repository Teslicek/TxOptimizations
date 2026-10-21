package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.teslicek.txoptimizations.mixin.ByteBufferResultAccessor;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.lwjgl.system.MemoryUtil;

public final class CompactTerrain {

    public static final int          VERTEX_SIZE = 20;
    public static final VertexFormat FORMAT      = VertexFormat.builder(0).addAttribute("PositionData", GpuFormat.RG32_UINT).addAttribute("Color", GpuFormat.RGBA8_UNORM).addAttribute("UVLight", GpuFormat.RG32_UINT).build();

    private static final int    SOURCE_SIZE    = 28;
    private static final int    COLOR_OFFSET   = 12;
    private static final int    UV_OFFSET      = 16;
    private static final int    LIGHT_OFFSET   = 24;
    private static final double POSITION_BIAS  = 8.0;
    private static final double POSITION_SCALE = 32768.0;
    private static final int    POSITION_BITS  = 20;
    private static final int    POSITION_MAX   = (1 << POSITION_BITS) - 1;
    private static final int    POSITION_SPLIT = 12;
    private static final int    POSITION_HIGH  = 8;
    private static final double UV_SCALE       = 8388608.0;
    private static final int    UV_MAX         = 1 << 23;
    private static final int    LIGHT_SHIFT    = 24;
    private static final int    LIGHT_MAX      = 255;
    private static final int    SHORT_BITS     = 16;

    private static volatile boolean active;

    private CompactTerrain() {
    }

    public static boolean active() {
        return active;
    }

    public static int bufferAlignment(int vanillaVertexSize) {
        active = PackedTerrain.isPackedSource();

        return active ? VERTEX_SIZE : vanillaVertexSize;
    }

    public static int vertexSize(ChunkSectionLayer layer) {
        return active ? VERTEX_SIZE : layer.pipeline(false).getVertexFormatBinding(0).getVertexSize();
    }

    public static void encode(MeshData mesh) {
        VertexFormat format = mesh.drawState().format();

        requireElement(format, "Position", GpuFormat.RGB32_FLOAT, 0);
        requireElement(format, "Color", GpuFormat.RGBA8_UNORM, COLOR_OFFSET);
        requireElement(format, "UV0", GpuFormat.RG32_FLOAT, UV_OFFSET);
        requireElement(format, "UV2", GpuFormat.RG16_SINT, LIGHT_OFFSET);

        if (format.getVertexSize() != SOURCE_SIZE)
            throw new IllegalStateException("Terrain vertices are " + format.getVertexSize() + " bytes instead of " + SOURCE_SIZE + " in " + format);

        int        vertices = mesh.drawState().vertexCount();
        ByteBuffer buffer   = mesh.vertexBuffer();

        if ((long) vertices * SOURCE_SIZE != buffer.remaining())
            throw new IllegalStateException(vertices + " terrain vertices of " + SOURCE_SIZE + " bytes do not fill " + buffer.remaining() + " bytes");

        encodeVertices(MemoryUtil.memAddress(buffer), vertices);
        ((ByteBufferResultAccessor) mesh.vertexBufferSlice()).txoptimizations$setSize(vertices * VERTEX_SIZE);
    }

    public static void encodeVertices(long base, int vertices) {
        for (int vertex = 0; vertex < vertices; vertex ++) {
            long source = base + (long) vertex * SOURCE_SIZE;
            int  x      = position(MemoryUtil.memGetFloat(source));
            int  y      = position(MemoryUtil.memGetFloat(source + 4L));
            int  z      = position(MemoryUtil.memGetFloat(source + 8L));
            int  color  = MemoryUtil.memGetInt(source + COLOR_OFFSET);
            int  u      = uv(MemoryUtil.memGetFloat(source + UV_OFFSET));
            int  v      = uv(MemoryUtil.memGetFloat(source + UV_OFFSET + 4L));
            int  light  = MemoryUtil.memGetInt(source + LIGHT_OFFSET);
            long target = base + (long) vertex * VERTEX_SIZE;

            MemoryUtil.memPutInt(target, x | y << POSITION_BITS);
            MemoryUtil.memPutInt(target + 4L, y >>> POSITION_SPLIT | z << POSITION_HIGH);
            MemoryUtil.memPutInt(target + 8L, color);
            MemoryUtil.memPutInt(target + 12L, u | light((short) light) << LIGHT_SHIFT);
            MemoryUtil.memPutInt(target + 16L, v | light((short) (light >> SHORT_BITS)) << LIGHT_SHIFT);
        }
    }

    private static int position(float value) {
        long quantized = Math.round((value + POSITION_BIAS) * POSITION_SCALE);

        if (quantized < 0 || quantized > POSITION_MAX)
            throw new IllegalStateException("Terrain vertex coordinate " + value + " is outside the compact range of -8 to 24 blocks around its section");

        return (int) quantized;
    }

    private static int uv(float value) {
        long quantized = Math.round(value * UV_SCALE);

        if (quantized < 0 || quantized > UV_MAX)
            throw new IllegalStateException("Terrain texture coordinate " + value + " is outside the atlas");

        return (int) quantized;
    }

    private static int light(short value) {
        if (value < 0 || value > LIGHT_MAX)
            throw new IllegalStateException("Terrain light coordinate " + value + " does not fit in a byte");

        return value;
    }

    private static void requireElement(VertexFormat format, String name, GpuFormat expected, int offset) {
        VertexFormatElement element = format.getElement(name);

        if (element == null || element.format() != expected || element.offset() != offset)
            throw new IllegalStateException("Terrain vertex format " + format + " has " + name + " as " + element + " instead of " + expected + " at byte " + offset);
    }
}
