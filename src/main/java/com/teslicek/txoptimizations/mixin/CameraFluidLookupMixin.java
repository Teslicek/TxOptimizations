package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
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
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Camera.class, priority = 999)
public abstract class CameraFluidLookupMixin {

    @Unique
    private static final int NEAR_PLANE_POINTS = 5;

    @Shadow
    private boolean initialized;

    @Shadow
    private Level level;

    @Shadow
    private Vec3 position;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    @Final
    private BlockPos.MutableBlockPos blockPosition;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private final BlockPos.MutableBlockPos txoptimizations$checkPos = new BlockPos.MutableBlockPos();

    @Unique
    private long txoptimizations$fluidFrame = -1L;

    @Unique
    private boolean txoptimizations$fluidInitialized;

    @Unique
    private Level txoptimizations$fluidLevel;

    @Unique
    private Vec3 txoptimizations$fluidPosition;

    @Unique
    private float txoptimizations$fluidXRot;

    @Unique
    private float txoptimizations$fluidYRot;

    @Unique
    private FogType txoptimizations$fluid;

    @Shadow
    public abstract Camera.NearPlane getNearPlane(float fov);

    @Overwrite
    public FogType getFluidInCamera() {
        if (this.txoptimizations$fluidFrame == ClientClock.frame() && this.txoptimizations$fluidInitialized == this.initialized && this.txoptimizations$fluidLevel == this.level && this.txoptimizations$fluidPosition == this.position && this.txoptimizations$fluidXRot == this.xRot && this.txoptimizations$fluidYRot == this.yRot)
            return this.txoptimizations$fluid;

        FogType fluid = this.txoptimizations$findFluid();

        this.txoptimizations$fluidFrame       = ClientClock.frame();
        this.txoptimizations$fluidInitialized = this.initialized;
        this.txoptimizations$fluidLevel       = this.level;
        this.txoptimizations$fluidPosition    = this.position;
        this.txoptimizations$fluidXRot        = this.xRot;
        this.txoptimizations$fluidYRot        = this.yRot;
        this.txoptimizations$fluid            = fluid;

        return fluid;
    }

    @Unique
    private FogType txoptimizations$findFluid() {
        if (!this.initialized)
            return FogType.NONE;

        FluidState cameraFluid = this.level.getFluidState(this.blockPosition);

        if (cameraFluid.is(FluidTags.WATER) && this.position.y < this.blockPosition.getY() + cameraFluid.getHeightForCamera(this.level, this.blockPosition))
            return FogType.WATER;

        CameraNearPlaneAccessor  plane    = (CameraNearPlaneAccessor) this.getNearPlane(this.minecraft.options.fov().get().intValue());
        Vec3                     forward  = plane.txoptimizations$forward();
        Vec3                     left     = plane.txoptimizations$left();
        Vec3                     up       = plane.txoptimizations$up();
        double                   topX     = forward.x + up.x;
        double                   topY     = forward.y + up.y;
        double                   topZ     = forward.z + up.z;
        double                   bottomX  = forward.x + -up.x;
        double                   bottomY  = forward.y + -up.y;
        double                   bottomZ  = forward.z + -up.z;
        BlockPos.MutableBlockPos checkPos = this.txoptimizations$checkPos.set(this.blockPosition);
        FluidState               fluid    = cameraFluid;
        BlockState               block    = null;

        for (int point = 0; point < NEAR_PLANE_POINTS; point ++) {
            double x = this.position.x + switch (point) {
                case 0 -> forward.x;
                case 1 -> topX + left.x;
                case 2 -> topX + -left.x;
                case 3 -> bottomX + left.x;
                default -> bottomX + -left.x;
            };
            double y = this.position.y + switch (point) {
                case 0 -> forward.y;
                case 1 -> topY + left.y;
                case 2 -> topY + -left.y;
                case 3 -> bottomY + left.y;
                default -> bottomY + -left.y;
            };
            double z = this.position.z + switch (point) {
                case 0 -> forward.z;
                case 1 -> topZ + left.z;
                case 2 -> topZ + -left.z;
                case 3 -> bottomZ + left.z;
                default -> bottomZ + -left.z;
            };
            int blockX = Mth.floor(x);
            int blockY = Mth.floor(y);
            int blockZ = Mth.floor(z);

            if (blockX != checkPos.getX() || blockY != checkPos.getY() || blockZ != checkPos.getZ()) {
                checkPos.set(blockX, blockY, blockZ);
                fluid = this.level.getFluidState(checkPos);
                block = null;
            }

            if (fluid.is(FluidTags.LAVA)) {
                if (y <= fluid.getHeightForCamera(this.level, checkPos) + checkPos.getY())
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
