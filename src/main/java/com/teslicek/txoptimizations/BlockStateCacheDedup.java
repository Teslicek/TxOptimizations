package com.teslicek.txoptimizations;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.booleans.BooleanArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public final class BlockStateCacheDedup {

    private static final Map<ArrayVoxelShape, ArrayVoxelShape> COLLISION_SHAPES = new Object2ObjectOpenCustomHashMap<>(new ShapeStrategy());
    private static final Map<boolean[], boolean[]>             FACE_STURDY      = new Object2ObjectOpenCustomHashMap<>(BooleanArrays.HASH_STRATEGY);

    private BlockStateCacheDedup() {
    }

    public static synchronized void deduplicate(BlockBehaviour.BlockStateBase.Cache cache) {
        cache.faceSturdy = FACE_STURDY.computeIfAbsent(cache.faceSturdy, Function.identity());

        if (!(cache.collisionShape instanceof ArrayVoxelShape shape))
            return;

        ArrayVoxelShape kept = COLLISION_SHAPES.computeIfAbsent(shape, Function.identity());

        if (kept == shape)
            return;

        shape.xs             = kept.xs;
        shape.ys             = kept.ys;
        shape.zs             = kept.zs;
        shape.shape          = kept.shape;
        shape.faces          = kept.faces;
        cache.collisionShape = kept;
    }

    private static final class ShapeStrategy implements Hash.Strategy<ArrayVoxelShape> {

        @Override
        public int hashCode(ArrayVoxelShape shape) {
            int hash = shape.xs.hashCode();
            hash = 31 * hash + shape.ys.hashCode();
            hash = 31 * hash + shape.zs.hashCode();

            return 31 * hash + occupancyHash(shape.shape);
        }

        @Override
        public boolean equals(ArrayVoxelShape a, ArrayVoxelShape b) {
            if (a == b)
                return true;

            if (a == null || b == null)
                return false;

            return a.xs.equals(b.xs) && a.ys.equals(b.ys) && a.zs.equals(b.zs) && sameOccupancy(a.shape, b.shape);
        }

        private static int occupancyHash(DiscreteVoxelShape shape) {
            int hash = shape.getXSize();
            hash = 31 * hash + shape.getYSize();
            hash = 31 * hash + shape.getZSize();

            for (int x = 0; x < shape.getXSize(); x ++)
                for (int y = 0; y < shape.getYSize(); y ++)
                    for (int z = 0; z < shape.getZSize(); z ++)
                        hash = 31 * hash + (shape.isFull(x, y, z) ? 1 : 0);

            return hash;
        }

        private static boolean sameOccupancy(DiscreteVoxelShape a, DiscreteVoxelShape b) {
            if (a.getXSize() != b.getXSize() || a.getYSize() != b.getYSize() || a.getZSize() != b.getZSize())
                return false;

            for (int x = 0; x < a.getXSize(); x ++)
                for (int y = 0; y < a.getYSize(); y ++)
                    for (int z = 0; z < a.getZSize(); z ++)
                        if (a.isFull(x, y, z) != b.isFull(x, y, z))
                            return false;

            return true;
        }
    }
}
