package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleLightCacheMixin {

    @Unique
    private static final int UNLOADED_LIGHT = 15728640;

    @Shadow
    @Final
    protected ClientLevel level;

    @Shadow
    protected double x;

    @Shadow
    protected double y;

    @Shadow
    protected double z;

    @Unique
    private static final BlockPos.MutableBlockPos LIGHT_POS = new BlockPos.MutableBlockPos();

    @Unique
    private long txoptimizations$lightTick = -1L;

    @Unique
    private double txoptimizations$lightX;

    @Unique
    private double txoptimizations$lightY;

    @Unique
    private double txoptimizations$lightZ;

    @Unique
    private int txoptimizations$light;

    @Overwrite
    protected int getLightCoords(float partialTick) {
        long tick = ClientClock.tick();

        if (this.txoptimizations$lightTick == tick && this.txoptimizations$lightX == this.x && this.txoptimizations$lightY == this.y && this.txoptimizations$lightZ == this.z)
            return this.txoptimizations$light;

        BlockPos pos = LIGHT_POS.set(this.x, this.y, this.z);

        this.txoptimizations$lightTick = tick;
        this.txoptimizations$lightX    = this.x;
        this.txoptimizations$lightY    = this.y;
        this.txoptimizations$lightZ    = this.z;
        this.txoptimizations$light     = this.level.hasChunkAt(pos) ? LightCoordsUtil.getLightCoords(this.level, pos) : UNLOADED_LIGHT;

        return this.txoptimizations$light;
    }
}
