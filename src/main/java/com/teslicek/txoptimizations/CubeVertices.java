package com.teslicek.txoptimizations;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.Direction;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

public final class CubeVertices {

    public static final int FACES    = 6;
    public static final int VERTICES = 24;
    public static final int VALUES   = 7 + VERTICES * 2;

    private static final int     FACE_VERTICES = 4;
    private static final int     CORNERS       = 8;
    private static final int     DOWN_FACE     = 0;
    private static final int     UP_FACE       = 1;
    private static final int     WEST_FACE     = 2;
    private static final int     ORIGIN        = 0;
    private static final int     FAR_BOTTOM    = 5;
    private static final int     NEAR_TOP      = 3;
    private static final int     X_BIT         = 1;
    private static final int     Y_BIT         = 2;
    private static final int     Z_BIT         = 4;
    private static final int     MIRROR        = 0;
    private static final int     BOUNDS        = 1;
    private static final int     UVS           = 7;
    private static final byte[]  FACE_CORNERS  = {5, 4, 0, 1, 3, 2, 6, 7, 0, 4, 6, 2, 1, 0, 2, 3, 5, 1, 3, 7, 4, 5, 7, 6};
    private static final Faces   PLAIN         = Faces.of(FACE_CORNERS, Direction.DOWN, Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH);
    private static final Faces   MIRRORED      = Faces.of(reversedFaces(FACE_CORNERS), Direction.DOWN, Direction.UP, Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH);
    private static final float[] X             = new float[CORNERS];
    private static final float[] Y             = new float[CORNERS];
    private static final float[] Z             = new float[CORNERS];

    private CubeVertices() {
    }

    public static float[] flatten(ModelPart.Polygon[] polygons) {
        Faces   faces  = faces(polygons);
        float[] values = new float[VALUES];

        values[MIRROR] = faces == MIRRORED ? 1.0F : 0.0F;

        ModelPart.Vertex[] down      = polygons[DOWN_FACE].vertices();
        ModelPart.Vertex   origin    = down[faces.origin()];
        ModelPart.Vertex   farBottom = down[faces.farBottom()];
        ModelPart.Vertex   nearTop   = polygons[UP_FACE].vertices()[faces.nearTop()];

        values[BOUNDS]     = origin.worldX();
        values[BOUNDS + 1] = origin.worldY();
        values[BOUNDS + 2] = origin.worldZ();
        values[BOUNDS + 3] = farBottom.worldX();
        values[BOUNDS + 4] = nearTop.worldY();
        values[BOUNDS + 5] = farBottom.worldZ();

        for (int face = 0; face < FACES; face ++) {
            ModelPart.Vertex[] vertices = polygons[face].vertices();

            for (int slot = 0; slot < FACE_VERTICES; slot ++) {
                int at = UVS + (face * FACE_VERTICES + slot) * 2;

                values[at]     = vertices[slot].u();
                values[at + 1] = vertices[slot].v();
            }
        }

        return values;
    }

    public static boolean tryPut(PoseStack.Pose pose, float[] values, VertexConsumer builder, int light, int overlay, int color) {
        VertexConsumer target  = builder;
        UvMapping      mapping = null;

        if (builder instanceof SpriteCoordinateExpander expander) {
            target  = expander.delegate();
            mapping = expander.mapping();
        }

        if (!(target instanceof DirectVertexBuffer direct))
            return false;

        VertexFormat format = direct.txoptimizations$format();

        if (format == DefaultVertexFormat.ENTITY) {
            long pointer = direct.txoptimizations$tryReserveVertices(format, VERTICES);

            if (pointer == MemoryUtil.NULL)
                return false;

            putEntity(pointer, pose, values, color, overlay, light, mapping);

            return true;
        }

        TexturedVertexLayout layout = TexturedVertexLayout.textured(format);

        if (layout == null)
            return false;

        long pointer = direct.txoptimizations$tryReserveVertices(format, VERTICES);

        if (pointer == MemoryUtil.NULL)
            return false;

        putTextured(pointer, layout, pose.pose(), values, color, light, mapping);

        return true;
    }

    public static void putEntity(long pointer, PoseStack.Pose pose, float[] values, int color, int overlay, int light, UvMapping mapping) {
        Faces faces = values[MIRROR] == 0.0F ? PLAIN : MIRRORED;

        corners(pose.pose(), values);
        NormalCache.selectAll(pose);

        byte[] corners     = faces.corners();
        int[]  normals     = faces.normals();
        int    colorBits   = VertexBytes.colorBits(color);
        long   coordinates = VertexBytes.pairBits(overlay, light);

        for (int face = 0; face < FACES; face ++) {
            int normal = NormalCache.selected(normals[face]);

            for (int slot = 0; slot < FACE_VERTICES; slot ++) {
                int   index  = face * FACE_VERTICES + slot;
                int   corner = corners[index];
                float u      = mapping == null ? values[UVS + index * 2] : mapping.getU(values[UVS + index * 2]);
                float v      = mapping == null ? values[UVS + index * 2 + 1] : mapping.getV(values[UVS + index * 2 + 1]);

                VertexBytes.putEntityBits(pointer, X[corner], Y[corner], Z[corner], colorBits, u, v, coordinates, normal);
                pointer += VertexBytes.ENTITY_STRIDE;
            }
        }
    }

    public static void putTextured(long pointer, TexturedVertexLayout layout, Matrix4f matrix, float[] values, int color, int light, UvMapping mapping) {
        Faces faces = values[MIRROR] == 0.0F ? PLAIN : MIRRORED;

        corners(matrix, values);

        byte[] corners = faces.corners();

        for (int face = 0; face < FACES; face ++) {
            for (int slot = 0; slot < FACE_VERTICES; slot ++) {
                int   index  = face * FACE_VERTICES + slot;
                int   corner = corners[index];
                float u      = mapping == null ? values[UVS + index * 2] : mapping.getU(values[UVS + index * 2]);
                float v      = mapping == null ? values[UVS + index * 2 + 1] : mapping.getV(values[UVS + index * 2 + 1]);

                layout.put(pointer, X[corner], Y[corner], Z[corner], color, u, v, light);
                pointer += layout.stride();
            }
        }
    }

    private static Faces faces(ModelPart.Polygon[] polygons) {
        if (polygons.length != FACES)
            throw new IllegalArgumentException("A cube with " + polygons.length + " faces has no shared corners");

        Vector3fc normal = polygons[WEST_FACE].normal();

        if (normal == Direction.WEST.getUnitVec3f())
            return PLAIN;

        if (normal == Direction.EAST.getUnitVec3f())
            return MIRRORED;

        throw new IllegalStateException("Cube face " + WEST_FACE + " faces " + normal + " instead of west or east");
    }

    private static void corners(Matrix4f matrix, float[] values) {
        float x0         = values[BOUNDS];
        float y0         = values[BOUNDS + 1];
        float z0         = values[BOUNDS + 2];
        float x1         = values[BOUNDS + 3];
        float y1         = values[BOUNDS + 4];
        float z1         = values[BOUNDS + 5];
        int   properties = matrix.properties();

        if ((properties & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            axis(X, X_BIT, x0, x1);
            axis(Y, Y_BIT, y0, y1);
            axis(Z, Z_BIT, z0, z1);

            return;
        }

        if ((properties & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            axis(X, X_BIT, x0 + matrix.m30(), x1 + matrix.m30());
            axis(Y, Y_BIT, y0 + matrix.m31(), y1 + matrix.m31());
            axis(Z, Z_BIT, z0 + matrix.m32(), z1 + matrix.m32());

            return;
        }

        component(X, matrix.m00(), matrix.m10(), matrix.m20(), matrix.m30(), x0, x1, y0, y1, z0, z1);
        component(Y, matrix.m01(), matrix.m11(), matrix.m21(), matrix.m31(), x0, x1, y0, y1, z0, z1);
        component(Z, matrix.m02(), matrix.m12(), matrix.m22(), matrix.m32(), x0, x1, y0, y1, z0, z1);
    }

    private static void axis(float[] values, int bit, float low, float high) {
        for (int corner = 0; corner < CORNERS; corner ++)
            values[corner] = (corner & bit) == 0 ? low : high;
    }

    private static void component(float[] values, float xFactor, float yFactor, float zFactor, float offset, float x0, float x1, float y0, float y1, float z0, float z1) {
        float near       = Math.fma(zFactor, z0, offset);
        float far        = Math.fma(zFactor, z1, offset);
        float nearBottom = Math.fma(yFactor, y0, near);
        float nearTop    = Math.fma(yFactor, y1, near);
        float farBottom  = Math.fma(yFactor, y0, far);
        float farTop     = Math.fma(yFactor, y1, far);

        values[0] = Math.fma(xFactor, x0, nearBottom);
        values[1] = Math.fma(xFactor, x1, nearBottom);
        values[2] = Math.fma(xFactor, x0, nearTop);
        values[3] = Math.fma(xFactor, x1, nearTop);
        values[4] = Math.fma(xFactor, x0, farBottom);
        values[5] = Math.fma(xFactor, x1, farBottom);
        values[6] = Math.fma(xFactor, x0, farTop);
        values[7] = Math.fma(xFactor, x1, farTop);
    }

    private static byte[] reversedFaces(byte[] corners) {
        byte[] reversed = new byte[corners.length];

        for (int index = 0; index < corners.length; index ++)
            reversed[index] = corners[index - index % FACE_VERTICES + FACE_VERTICES - 1 - index % FACE_VERTICES];

        return reversed;
    }

    private record Faces(byte[] corners, int[] normals, int origin, int farBottom, int nearTop) {

        private static Faces of(byte[] corners, Direction... facings) {
            int[] normals = new int[FACES];

            for (int face = 0; face < FACES; face ++)
                normals[face] = facings[face].ordinal();

            return new Faces(corners, normals, slot(corners, DOWN_FACE, ORIGIN), slot(corners, DOWN_FACE, FAR_BOTTOM), slot(corners, UP_FACE, NEAR_TOP));
        }

        private static int slot(byte[] corners, int face, int corner) {
            for (int slot = 0; slot < FACE_VERTICES; slot ++) {
                if (corners[face * FACE_VERTICES + slot] == corner)
                    return slot;
            }

            throw new IllegalStateException("Cube face " + face + " has no corner " + corner);
        }
    }
}
