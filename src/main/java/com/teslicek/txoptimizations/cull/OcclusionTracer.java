package com.teslicek.txoptimizations.cull;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.BitSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

final class OcclusionTracer {

    private static final double EXPANSION = 0.5;
    private static final double NEAR      = 0.05;
    private static final double MIDDLE    = 0.5;
    private static final double FAR       = 0.95;

    private static final int MIN_X = 1;
    private static final int MAX_X = 2;
    private static final int MIN_Y = 4;
    private static final int MAX_Y = 8;
    private static final int MIN_Z = 16;
    private static final int MAX_Z = 32;

    private static final int OUTSIDE = -1;
    private static final int UNKNOWN = 0;
    private static final int VISIBLE = 1;
    private static final int HIDDEN  = 2;

    private static final int INSIDE   = 0;
    private static final int POSITIVE = 1;
    private static final int NEGATIVE = 2;

    private static final double[] POINTS = {
        NEAR, NEAR, NEAR,
        NEAR, FAR, NEAR,
        NEAR, FAR, FAR,
        NEAR, NEAR, FAR,
        FAR, NEAR, NEAR,
        FAR, FAR, NEAR,
        FAR, FAR, FAR,
        FAR, NEAR, FAR,
        NEAR, MIDDLE, MIDDLE,
        MIDDLE, NEAR, MIDDLE,
        MIDDLE, MIDDLE, NEAR,
        FAR, MIDDLE, MIDDLE,
        MIDDLE, FAR, MIDDLE,
        MIDDLE, MIDDLE, FAR
    };

    private static final int[] FACES       = {MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z};
    private static final int[] FACE_POINTS = {1 | 1 << 8, 1 | 1 << 9, 1 | 1 << 10, 1 << 4 | 1 << 11, 1 << 1 | 1 << 12, 1 << 2 | 1 << 13};
    private static final int[] EDGE_POINTS = {1 << 1 | 1 << 4 | 1 << 5, 1 << 3 | 1 << 4 | 1 << 7, 1 << 1 | 1 << 4 | 1 << 5, 1 << 5 | 1 << 6 | 1 << 7, 1 << 2 | 1 << 5 | 1 << 6, 1 << 3 | 1 << 6 | 1 << 7};

    private final int          reach;
    private final int          size;
    private final byte[]       cache;
    private final IntArrayList touched = new IntArrayList();
    private final BitSet       skipped = new BitSet();
    private final double[]     viewer  = new double[3];
    private final int[]        camera  = new int[3];
    private final double[]     target  = new double[3];
    private final double[]     delta   = new double[3];
    private final double[]     next    = new double[3];
    private final double[]     step    = new double[3];
    private final int[]        advance = new int[3];

    private ClientLevel level;
    private LevelChunk  chunk;
    private boolean     chunkCached;
    private int         chunkX;
    private int         chunkZ;
    private boolean     hasLastHit;
    private int         lastHitX;
    private int         lastHitY;
    private int         lastHitZ;

    OcclusionTracer(int reach) {
        this.reach = reach;
        this.size  = reach * 2;
        this.cache = new byte[this.size * this.size * this.size / 4];
    }

    void begin(ClientLevel currentLevel, Vec3 position) {
        for (int i = 0; i < this.touched.size(); i ++)
            this.cache[this.touched.getInt(i)] = 0;

        this.touched.clear();
        this.level       = currentLevel;
        this.chunkCached = false;
        this.viewer[0]   = position.x;
        this.viewer[1]   = position.y;
        this.viewer[2]   = position.z;
        this.camera[0]   = Mth.floor(position.x);
        this.camera[1]   = Mth.floor(position.y);
        this.camera[2]   = Mth.floor(position.z);
    }

    void finish() {
        this.level = null;
        this.chunk = null;
    }

    boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int boxMinX   = Mth.floor(minX - EXPANSION);
        int boxMinY   = Mth.floor(minY - EXPANSION);
        int boxMinZ   = Mth.floor(minZ - EXPANSION);
        int boxMaxX   = Mth.floor(maxX + EXPANSION);
        int boxMaxY   = Mth.floor(maxY + EXPANSION);
        int boxMaxZ   = Mth.floor(maxZ + EXPANSION);
        int relationX = relation(boxMinX, boxMaxX, this.camera[0]);
        int relationY = relation(boxMinY, boxMaxY, this.camera[1]);
        int relationZ = relation(boxMinZ, boxMaxZ, this.camera[2]);

        if (relationX == INSIDE && relationY == INSIDE && relationZ == INSIDE)
            return true;

        if (this.index(boxMinX, boxMinY, boxMinZ) < 0 || this.index(boxMaxX, boxMaxY, boxMaxZ) < 0)
            return true;

        this.skipped.clear();

        int id = 0;

        for (int x = boxMinX; x <= boxMaxX; x ++) {
            for (int y = boxMinY; y <= boxMaxY; y ++) {
                for (int z = boxMinZ; z <= boxMaxZ; z ++) {
                    int state = this.getState(x, y, z);

                    if (state == VISIBLE)
                        return true;

                    if (state != UNKNOWN)
                        this.skipped.set(id);

                    id ++;
                }
            }
        }

        this.hasLastHit = false;
        id              = 0;

        for (int x = boxMinX; x <= boxMaxX; x ++) {
            int edgeX = (x == boxMinX ? MIN_X : 0) | (x == boxMaxX ? MAX_X : 0);
            int faceX = (x == boxMinX && relationX == POSITIVE ? MIN_X : 0) | (x == boxMaxX && relationX == NEGATIVE ? MAX_X : 0);

            for (int y = boxMinY; y <= boxMaxY; y ++) {
                int edgeY = edgeX | (y == boxMinY ? MIN_Y : 0) | (y == boxMaxY ? MAX_Y : 0);
                int faceY = faceX | (y == boxMinY && relationY == POSITIVE ? MIN_Y : 0) | (y == boxMaxY && relationY == NEGATIVE ? MAX_Y : 0);

                for (int z = boxMinZ; z <= boxMaxZ; z ++) {
                    int edge = edgeY | (z == boxMinZ ? MIN_Z : 0) | (z == boxMaxZ ? MAX_Z : 0);
                    int face = faceY | (z == boxMinZ && relationZ == POSITIVE ? MIN_Z : 0) | (z == boxMaxZ && relationZ == NEGATIVE ? MAX_Z : 0);

                    if (face != 0 && !this.skipped.get(id) && this.isVoxelVisible(x, y, z, edge, face))
                        return true;

                    id ++;
                }
            }
        }

        return false;
    }

    private boolean isVoxelVisible(int x, int y, int z, int edge, int face) {
        int points = 0;

        for (int i = 0; i < FACES.length; i ++) {
            if ((face & FACES[i]) == 0)
                continue;

            points |= FACE_POINTS[i];

            if ((edge & ~FACES[i]) != 0)
                points |= EDGE_POINTS[i];
        }

        for (int remaining = points; remaining != 0; remaining &= remaining - 1) {
            int point = Integer.numberOfTrailingZeros(remaining) * 3;

            this.target[0] = x + POINTS[point];
            this.target[1] = y + POINTS[point + 1];
            this.target[2] = z + POINTS[point + 2];
            this.delta[0]  = this.viewer[0] - this.target[0];
            this.delta[1]  = this.viewer[1] - this.target[1];
            this.delta[2]  = this.viewer[2] - this.target[2];

            if (this.hasLastHit && this.crossesLastHit())
                continue;

            if (this.traceRay()) {
                this.setState(x, y, z, VISIBLE);

                return true;
            }

            this.hasLastHit = true;
        }

        this.setState(x, y, z, HIDDEN);

        return false;
    }

    private boolean crossesLastHit() {
        double inverseX = 1.0 / this.delta[0];
        double inverseY = 1.0 / this.delta[1];
        double inverseZ = 1.0 / this.delta[2];
        double nearX    = (this.lastHitX - this.viewer[0]) * inverseX;
        double farX     = (this.lastHitX + 1 - this.viewer[0]) * inverseX;
        double nearY    = (this.lastHitY - this.viewer[1]) * inverseY;
        double farY     = (this.lastHitY + 1 - this.viewer[1]) * inverseY;
        double nearZ    = (this.lastHitZ - this.viewer[2]) * inverseZ;
        double farZ     = (this.lastHitZ + 1 - this.viewer[2]) * inverseZ;
        double entry    = Math.max(Math.max(Math.min(nearX, farX), Math.min(nearY, farY)), Math.min(nearZ, farZ));
        double exit     = Math.min(Math.min(Math.max(nearX, farX), Math.max(nearY, farY)), Math.max(nearZ, farZ));

        return !(exit > 0.0) && !(entry > exit);
    }

    private boolean traceRay() {
        int steps = 1;

        for (int axis = 0; axis < 3; axis ++) {
            double distance = Math.abs(this.delta[axis]);

            this.step[axis] = 1.0 / distance;

            if (distance == 0.0) {
                this.advance[axis] = 0;
                this.next[axis]    = this.step[axis];
            } else if (this.target[axis] > this.viewer[axis]) {
                this.advance[axis] = 1;
                this.next[axis]    = (float) ((this.camera[axis] + 1 - this.viewer[axis]) * this.step[axis]);
                steps             += Mth.floor(this.target[axis]) - this.camera[axis];
            } else {
                this.advance[axis] = -1;
                this.next[axis]    = (float) ((this.viewer[axis] - this.camera[axis]) * this.step[axis]);
                steps             += this.camera[axis] - Mth.floor(this.target[axis]);
            }
        }

        return this.walk(steps);
    }

    private boolean walk(int steps) {
        int     x        = this.camera[0];
        int     y        = this.camera[1];
        int     z        = this.camera[2];
        double  nextX    = this.next[0];
        double  nextY    = this.next[1];
        double  nextZ    = this.next[2];
        boolean clipping = true;

        for (int remaining = steps; remaining > 1; remaining --) {
            int state = this.getState(x, y, z);

            if (state == HIDDEN && !clipping)
                return this.hit(x, y, z);

            if (state == UNKNOWN) {
                if (!this.isOpaque(x, y, z)) {
                    clipping = false;
                    this.setState(x, y, z, VISIBLE);
                } else if (!clipping) {
                    this.setState(x, y, z, HIDDEN);

                    return this.hit(x, y, z);
                }
            }

            if (state == VISIBLE)
                clipping = false;

            if (nextY < nextX && nextY < nextZ) {
                y     += this.advance[1];
                nextY += this.step[1];
            } else if (nextX < nextY && nextX < nextZ) {
                x     += this.advance[0];
                nextX += this.step[0];
            } else {
                z     += this.advance[2];
                nextZ += this.step[2];
            }
        }

        return true;
    }

    private boolean hit(int x, int y, int z) {
        this.lastHitX = x;
        this.lastHitY = y;
        this.lastHitZ = z;

        return false;
    }

    private boolean isOpaque(int x, int y, int z) {
        int currentChunkX = x >> 4;
        int currentChunkZ = z >> 4;

        if (!this.chunkCached || currentChunkX != this.chunkX || currentChunkZ != this.chunkZ) {
            this.chunk       = this.level.getChunkSource().getChunk(currentChunkX, currentChunkZ, ChunkStatus.FULL, false);
            this.chunkX      = currentChunkX;
            this.chunkZ      = currentChunkZ;
            this.chunkCached = true;
        }

        if (this.chunk == null)
            return false;

        LevelChunkSection[] sections     = this.chunk.getSections();
        int                 sectionIndex = this.chunk.getSectionIndex(y);

        if (sectionIndex < 0 || sectionIndex >= sections.length)
            return false;

        LevelChunkSection section = sections[sectionIndex];

        return !section.hasOnlyAir() && section.getBlockState(x & 15, y & 15, z & 15).isSolidRender();
    }

    private int getState(int x, int y, int z) {
        int index = this.index(x, y, z);

        if (index < 0)
            return OUTSIDE;

        return this.cache[index >> 2] >> ((index & 3) << 1) & 3;
    }

    private void setState(int x, int y, int z, int state) {
        int index = this.index(x, y, z);
        int entry = index >> 2;

        if (this.cache[entry] == 0)
            this.touched.add(entry);

        this.cache[entry] |= (byte) (state << ((index & 3) << 1));
    }

    private int index(int x, int y, int z) {
        int relativeX = x - this.camera[0];
        int relativeY = y - this.camera[1];
        int relativeZ = z - this.camera[2];
        int limit     = this.reach - 2;

        if (Math.abs(relativeX) > limit || Math.abs(relativeY) > limit || Math.abs(relativeZ) > limit)
            return -1;

        return relativeX + this.reach + (relativeY + this.reach) * this.size + (relativeZ + this.reach) * this.size * this.size;
    }

    private static int relation(int min, int max, int position) {
        if (min > position)
            return POSITIVE;

        return max < position ? NEGATIVE : INSIDE;
    }
}
