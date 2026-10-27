package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParticleSnapshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleSnapshotMixin extends Particle implements ParticleSnapshot {

    @Shadow
    protected float quadSize;

    @Shadow
    protected float rCol;

    @Shadow
    protected float gCol;

    @Shadow
    protected float bCol;

    @Shadow
    protected float alpha;

    @Shadow
    protected float roll;

    @Shadow
    protected float oRoll;

    protected SingleQuadParticleSnapshotMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Shadow
    public abstract SingleQuadParticle.FacingCameraMode getFacingCameraMode();

    @Shadow
    public abstract float getQuadSize(float partialTickTime);

    @Shadow
    protected abstract SingleQuadParticle.Layer getLayer();

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    @Override
    public void txoptimizations$snapshot(float[] floats, int floatOffset, int[] ints, int intOffset, Object[] refs, int refOffset, double[] previous, int previousOffset) {
        int flags = ParticleSnapshot.CLASS_FLAGS.get(this.getClass());

        ints[intOffset + FLAGS] = flags;

        if ((flags & STORED) == 0)
            return;

        floats[floatOffset + O_ROLL] = this.oRoll;
        floats[floatOffset + ROLL]   = this.roll;
        floats[floatOffset + SIZE]   = this.quadSize;
        floats[floatOffset + U0]     = this.getU0();
        floats[floatOffset + U1]     = this.getU1();
        floats[floatOffset + V0]     = this.getV0();
        floats[floatOffset + V1]     = this.getV1();
        ints[intOffset + COLOR]      = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);
        ints[intOffset + LIGHT]      = (flags & OWN_LIGHT) == 0 ? this.getLightCoords(0.0F) : 0;
        refs[refOffset + LAYER]      = this.getLayer();
        refs[refOffset + FACING]     = this.getFacingCameraMode();
        previous[previousOffset]     = this.xo;
        previous[previousOffset + 1] = this.yo;
        previous[previousOffset + 2] = this.zo;
    }

    @Override
    public int txoptimizations$lightCoords(float partialTickTime) {
        return this.getLightCoords(partialTickTime);
    }
}
