package com.teslicek.txoptimizations.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CubeVoxelShape.class)
public abstract class CubeVoxelShapeCoordinateMixin extends VoxelShape {

    protected CubeVoxelShapeCoordinateMixin(DiscreteVoxelShape shape) {
        super(shape);
    }

    @Override
    protected double get(Direction.Axis axis, int index) {
        return (double) index / (double) this.shape.getSize(axis);
    }
}
