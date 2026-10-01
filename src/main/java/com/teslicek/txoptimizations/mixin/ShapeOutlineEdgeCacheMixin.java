package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShapeOutlineFeatureRenderer.class)
public abstract class ShapeOutlineEdgeCacheMixin {

    @Unique
    private static final int CACHED_SHAPES = 8;

    @Unique
    private static final int LINE_VALUES = 6;

    @Unique
    private final VoxelShape[] txoptimizations$shapes = new VoxelShape[CACHED_SHAPES];

    @Unique
    private final double[][] txoptimizations$edges = new double[CACHED_SHAPES][];

    @Unique
    private int txoptimizations$nextSlot;

    @WrapOperation(method = "buildGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllEdges(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
    private void txoptimizations$replayEdges(VoxelShape shape, Shapes.DoubleLineConsumer consumer, Operation<Void> original) {
        double[] edges = this.txoptimizations$edgesOf(shape, original);

        for (int i = 0; i < edges.length; i += LINE_VALUES)
            consumer.consume(edges[i], edges[i + 1], edges[i + 2], edges[i + 3], edges[i + 4], edges[i + 5]);
    }

    @Unique
    private double[] txoptimizations$edgesOf(VoxelShape shape, Operation<Void> original) {
        for (int slot = 0; slot < CACHED_SHAPES; slot ++)
            if (this.txoptimizations$shapes[slot] == shape)
                return this.txoptimizations$edges[slot];

        DoubleArrayList recorded = new DoubleArrayList();

        original.call(shape, (Shapes.DoubleLineConsumer) (x1, y1, z1, x2, y2, z2) -> {
            recorded.add(x1);
            recorded.add(y1);
            recorded.add(z1);
            recorded.add(x2);
            recorded.add(y2);
            recorded.add(z2);
        });

        double[] edges = recorded.toDoubleArray();

        this.txoptimizations$shapes[this.txoptimizations$nextSlot] = shape;
        this.txoptimizations$edges[this.txoptimizations$nextSlot]  = edges;
        this.txoptimizations$nextSlot                               = (this.txoptimizations$nextSlot + 1) % CACHED_SHAPES;

        return edges;
    }
}
