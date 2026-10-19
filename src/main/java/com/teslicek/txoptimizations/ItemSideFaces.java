package com.teslicek.txoptimizations;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

public final class ItemSideFaces {

    private static final int         UP         = 0;
    private static final int         DOWN       = 1;
    private static final int         LEFT       = 2;
    private static final int         RIGHT      = 3;
    private static final int         SIDES      = 4;
    private static final Direction[] DIRECTIONS = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
    private static final float       UV_INSET   = 0.1F;
    private static final float       LAYER_BIAS = 0.005F;
    private static final float       MIN_Z      = 7.5F;
    private static final float       MAX_Z      = 8.5F;
    private static final float       MODEL_SIZE = 16.0F;

    private ItemSideFaces() {
    }

    public static void bake(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo) {
        SpriteContents sprite  = materialInfo.sprite().contents();
        int            width   = sprite.width();
        int            height  = sprite.height();
        boolean[]      faces   = new boolean[SIDES * width * height];
        Baking         baking  = new Baking(builder, interner, modelState, materialInfo, MODEL_SIZE / width, MODEL_SIZE / height, materialInfo.tintIndex() * LAYER_BIAS);

        sprite.getUniqueFrames().forEach(frame -> markFaces(sprite, frame, width, height, faces));

        for (int side = UP; side <= DOWN; side ++) {
            for (int y = 0; y < height; y ++)
                bakeRuns(baking, faces, side, y, width, width, height);
        }

        for (int side = LEFT; side <= RIGHT; side ++) {
            for (int x = 0; x < width; x ++)
                bakeRuns(baking, faces, side, x, height, width, height);
        }
    }

    private static void markFaces(SpriteContents sprite, int frame, int width, int height, boolean[] faces) {
        for (int y = 0; y < height; y ++) {
            for (int x = 0; x < width; x ++) {
                if (isTransparent(sprite, frame, x, y, width, height))
                    continue;

                for (int side = 0; side < SIDES; side ++) {
                    Direction direction = DIRECTIONS[side];

                    if (isTransparent(sprite, frame, x - direction.getStepX(), y - direction.getStepY(), width, height))
                        faces[index(side, x, y, width, height)] = true;
                }
            }
        }
    }

    private static void bakeRuns(Baking baking, boolean[] faces, int side, int line, int length, int width, int height) {
        int position = 0;

        while (position < length) {
            if (!faces[runIndex(side, line, position, width, height)]) {
                position ++;
                continue;
            }

            int min = position;

            while (position + 1 < length && faces[runIndex(side, line, position + 1, width, height)])
                position ++;

            baking.bake(side, line, min, position);
            position ++;
        }
    }

    private static boolean isTransparent(SpriteContents sprite, int frame, int x, int y, int width, int height) {
        return x < 0 || y < 0 || x >= width || y >= height || sprite.isTransparent(frame, x, y);
    }

    private static int runIndex(int side, int line, int position, int width, int height) {
        return side == UP || side == DOWN ? index(side, position, line, width, height) : index(side, line, position, width, height);
    }

    private static int index(int side, int x, int y, int width, int height) {
        return (side * height + y) * width + x;
    }

    private record Baking(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo, float xScale, float yScale, float bias) {

        void bake(int side, int anchor, int min, int max) {
            boolean horizontal = side == UP || side == DOWN;
            float   x          = horizontal ? min : anchor;
            float   y          = horizontal ? anchor : min;
            float   length     = max - min + 1;
            float   u0         = horizontal ? x : x + UV_INSET;
            float   v0         = horizontal ? y + UV_INSET : y + length;
            float   u1         = horizontal ? x + length : x + 1.0F - UV_INSET;
            float   v1         = horizontal ? y + 1.0F - UV_INSET : y;
            float   startX     = x;
            float   startY     = y;
            float   endX       = x;
            float   endY       = y;

            switch (side) {
                case UP -> endX = x + length;
                case LEFT -> endY = y + length;
                case DOWN -> {
                    startY = y + 1.0F;
                    endY   = y + 1.0F;
                    endX   = x + length;
                }
                case RIGHT -> {
                    startX = x + 1.0F;
                    endX   = x + 1.0F;
                    endY   = y + length;
                }
                default -> throw new IllegalStateException("Unknown item side " + side);
            }

            startX = startX * this.xScale;
            startY = MODEL_SIZE - startY * this.yScale;
            endX   = endX * this.xScale;
            endY   = MODEL_SIZE - endY * this.yScale;

            switch (side) {
                case UP -> {
                    startX -= this.bias;
                    endX   += this.bias;
                    startY += this.bias;
                    endY    = startY;
                }
                case LEFT -> {
                    startY += this.bias;
                    endY   -= this.bias;
                    startX -= this.bias;
                    endX    = startX;
                }
                case DOWN -> {
                    startX -= this.bias;
                    endX   += this.bias;
                    endY   -= this.bias;
                    startY  = endY;
                }
                case RIGHT -> {
                    startY += this.bias;
                    endY   -= this.bias;
                    endX   += this.bias;
                    startX  = endX;
                }
                default -> throw new IllegalStateException("Unknown item side " + side);
            }

            CuboidFace.UVs uvs = new CuboidFace.UVs(u0 * this.xScale, v0 * this.yScale, u1 * this.xScale, v1 * this.yScale);

            this.builder.addUnculledFace(FaceBakery.bakeQuad(this.interner, new Vector3f(startX, startY, MIN_Z), new Vector3f(endX, endY, MAX_Z), uvs, Quadrant.R0, this.materialInfo, DIRECTIONS[side], this.modelState, null));
        }
    }
}
