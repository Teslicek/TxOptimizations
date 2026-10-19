package com.teslicek.txoptimizations;

public final class DrawRecords {

    public static final int NO_BUFFER     = -1;
    public static final int VERTEX_BUFFER = 0;
    public static final int FIRST_VERTEX  = 1;
    public static final int INDEX_COUNT   = 2;
    public static final int FACING        = 3;
    public static final int INDEX_FORMAT  = 3;
    public static final int INDEX_BUFFER  = 4;
    public static final int FIRST_INDEX   = 5;

    private static final int FLAGS            = 0;
    private static final int HEADER           = 1;
    private static final int MAX_LAYERS       = 3;
    private static final int LAYER_MASK       = (1 << MAX_LAYERS) - 1;
    private static final int DRAWABLE_SHIFT   = MAX_LAYERS;
    private static final int OFFSET_SHIFT     = 8;
    private static final int OFFSET_BITS      = 8;
    private static final int OFFSET_MASK      = (1 << OFFSET_BITS) - 1;
    private static final int OPAQUE_SIZE      = FACING + FacingCulling.RANGES;
    private static final int TRANSLUCENT_SIZE = FIRST_INDEX + 1;
    private static final int CUSTOM_INDEX     = 1;
    private static final int INDEX_TYPE_SHIFT = 1;

    private DrawRecords() {
    }

    public static int[] create(int rendered, boolean[] translucent) {
        if (translucent.length > MAX_LAYERS || (rendered & ~LAYER_MASK) != 0 || rendered == 0)
            throw new IllegalStateException("A draw record holds up to " + MAX_LAYERS + " layers but got layers " + Integer.toBinaryString(rendered) + " of " + translucent.length);

        int size  = HEADER;
        int flags = rendered;

        for (int layer = 0; layer < translucent.length; layer ++) {
            if ((rendered & 1 << layer) == 0)
                continue;

            flags |= size << OFFSET_SHIFT + layer * OFFSET_BITS;
            size  += translucent[layer] ? TRANSLUCENT_SIZE : OPAQUE_SIZE;
        }

        if (size > OFFSET_MASK)
            throw new IllegalStateException("A draw record of " + size + " ints does not fit its offsets");

        int[] record = new int[size];

        record[FLAGS] = flags;

        return record;
    }

    public static int flags(int[] record) {
        return record[FLAGS];
    }

    public static boolean rendered(int flags, int layer) {
        return (flags & 1 << layer) != 0;
    }

    public static int drawable(int flags) {
        return flags >>> DRAWABLE_SHIFT & LAYER_MASK;
    }

    public static int block(int flags, int layer) {
        return flags >>> OFFSET_SHIFT + layer * OFFSET_BITS & OFFSET_MASK;
    }

    public static boolean customIndex(int format) {
        return (format & CUSTOM_INDEX) != 0;
    }

    public static int indexType(int format) {
        return format >>> INDEX_TYPE_SHIFT;
    }

    public static int facing(int[] record, int layer) {
        return requireBlock(record, layer) + FACING;
    }

    public static void setDraw(int[] record, int layer, boolean translucent, int indexCount, boolean customIndex, int indexType) {
        int block = requireBlock(record, layer);

        record[block + VERTEX_BUFFER] = NO_BUFFER;
        record[block + INDEX_COUNT]   = indexCount;

        if (!translucent) {
            if (customIndex)
                throw new IllegalStateException("Opaque section geometry of layer " + layer + " has its own index buffer, but its facing groups index the shared quad buffer");

            return;
        }

        record[block + INDEX_FORMAT] = (customIndex ? CUSTOM_INDEX : 0) | indexType << INDEX_TYPE_SHIFT;
        record[block + INDEX_BUFFER] = NO_BUFFER;
    }

    public static void setDrawable(int[] record, int layer, int vertexBuffer, int firstVertex, int indexBuffer, int firstIndex, boolean translucent) {
        int block = requireBlock(record, layer);

        record[block + VERTEX_BUFFER] = vertexBuffer;
        record[block + FIRST_VERTEX]  = firstVertex;

        if (translucent) {
            record[block + INDEX_BUFFER] = indexBuffer;
            record[block + FIRST_INDEX]  = firstIndex;
        } else if (indexBuffer != NO_BUFFER) {
            throw new IllegalStateException("Opaque section geometry of layer " + layer + " cannot use index buffer " + indexBuffer);
        }

        record[FLAGS] |= 1 << DRAWABLE_SHIFT + layer;
    }

    public static void clearDrawable(int[] record, int layer, boolean translucent) {
        int block = requireBlock(record, layer);

        record[block + VERTEX_BUFFER] = NO_BUFFER;
        record[block + FIRST_VERTEX]  = 0;

        if (translucent) {
            record[block + INDEX_BUFFER] = NO_BUFFER;
            record[block + FIRST_INDEX]  = 0;
        }

        record[FLAGS] &= ~(1 << DRAWABLE_SHIFT + layer);
    }

    private static int requireBlock(int[] record, int layer) {
        if (!rendered(record[FLAGS], layer))
            throw new IllegalStateException("The draw record holds no layer " + layer);

        return block(record[FLAGS], layer);
    }
}
