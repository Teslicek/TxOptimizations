package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Camera.class)
public abstract class CameraFluidLookupMixin {

    @Shadow
    private boolean initialized;

    @Shadow
    private Level level;

    @Shadow
    private Vec3 position;

    @Shadow
    @Final
    private BlockPos.MutableBlockPos blockPosition;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract Camera.NearPlane getNearPlane(float fov);

    @Overwrite
    public FogType getFluidInCamera() {
        if (!this.initialized)
            return FogType.NONE;

        FluidState cameraFluid = this.level.getFluidState(this.blockPosition);

        if (cameraFluid.is(FluidTags.WATER) && this.position.y < this.blockPosition.getY() + cameraFluid.getHeightForCamera(this.level, this.blockPosition))
            return FogType.WATER;

        Camera.NearPlane plane   = this.getNearPlane(this.minecraft.options.fov().get().intValue());
        Vec3[]           points  = {((CameraNearPlaneAccessor) plane).txoptimizations$forward(), plane.getTopLeft(), plane.getTopRight(), plane.getBottomLeft(), plane.getBottomRight()};
        BlockPos         lastPos = this.blockPosition.immutable();
        FluidState       fluid   = cameraFluid;
        BlockState       block   = null;

        for (Vec3 point : points) {
            Vec3     offsetPos = this.position.add(point);
            BlockPos checkPos  = BlockPos.containing(offsetPos);

            if (!checkPos.equals(lastPos)) {
                lastPos = checkPos;
                fluid   = this.level.getFluidState(checkPos);
                block   = null;
            }

            if (fluid.is(FluidTags.LAVA)) {
                if (offsetPos.y <= fluid.getHeightForCamera(this.level, checkPos) + checkPos.getY())
                    return FogType.LAVA;

                continue;
            }

            if (block == null)
                block = this.level.getBlockState(checkPos);

            if (block.is(Blocks.POWDER_SNOW))
                return FogType.POWDER_SNOW;
        }

        return FogType.NONE;
    }
}
