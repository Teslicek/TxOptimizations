package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.FrustumBoxTest;
import com.teslicek.txoptimizations.FrustumPlanes;
import com.teslicek.txoptimizations.ParticleLightShare;
import com.teslicek.txoptimizations.ParticleGroupChanges;
import com.teslicek.txoptimizations.ParticleSnapshot;
import java.util.Arrays;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = QuadParticleGroup.class, priority = 999)
public abstract class QuadParticleGroupPositionsMixin extends ParticleGroup<SingleQuadParticle> {

    @Unique
    private static final int AXES = 3;

    @Unique
    private static final int MAX_PARTICLES = 16384;

    @Unique
    private static final int BLOCK_SHIFT = 6;

    @Unique
    private static final int BLOCK = 1 << BLOCK_SHIFT;

    @Unique
    private static final int BOUNDS = 6;

    @Unique
    private static final double[] PLANES = FrustumPlanes.newPlanes();

    @Unique
    private static final Quaternionf ROTATION = new Quaternionf();

    @Shadow
    @Final
    private ParticleRenderType particleType;

    @Shadow
    @Final
    private QuadParticleRenderState particleTypeRenderState;

    @Unique
    private double[] txoptimizations$positions = new double[0];

    @Unique
    private double[] txoptimizations$previous = new double[0];

    @Unique
    private float[] txoptimizations$floats = new float[0];

    @Unique
    private int[] txoptimizations$ints = new int[0];

    @Unique
    private Object[] txoptimizations$refs = new Object[0];

    @Unique
    private Object[] txoptimizations$order = new Object[0];

    @Unique
    private double[] txoptimizations$bounds = new double[0];

    @Unique
    private int txoptimizations$count;

    @Unique
    private int txoptimizations$snapshotChanges = -1;

    @Unique
    private long txoptimizations$snapshotTick = -1L;

    protected QuadParticleGroupPositionsMixin(ParticleEngine engine) {
        super(engine);
    }

    @Overwrite
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        this.txoptimizations$snapshot();

        double[] positions = this.txoptimizations$positions;
        double[] bounds    = this.txoptimizations$bounds;
        Object[] order     = this.txoptimizations$order;
        int      count     = this.txoptimizations$count;
        double[] planes    = PLANES;
        boolean  vanilla   = frustum.getClass() == Frustum.class;

        if (vanilla)
            ((FrustumBoxTest) frustum).txoptimizations$capturePlanes(planes);

        for (int start = 0; start < count; start += BLOCK) {
            int at    = (start >> BLOCK_SHIFT) * BOUNDS;
            int shape = vanilla ? FrustumPlanes.classifyPoints(planes, bounds[at], bounds[at + 1], bounds[at + 2], bounds[at + 3], bounds[at + 4], bounds[at + 5]) : FrustumBoxTest.MIXED;

            if (shape == FrustumBoxTest.OUTSIDE)
                continue;

            int end = Math.min(start + BLOCK, count);

            for (int index = start; index < end; index ++) {
                if (shape == FrustumBoxTest.INSIDE || frustum.pointInFrustum(positions[index * AXES], positions[index * AXES + 1], positions[index * AXES + 2]))
                    this.txoptimizations$extract((SingleQuadParticle) order[index], index, camera, partialTickTime);
            }
        }

        return this.particleTypeRenderState;
    }

    @Unique
    private void txoptimizations$snapshot() {
        int  changes = ((ParticleGroupChanges) (Object) this).txoptimizations$changes();
        long tick    = ClientClock.tick();

        if (changes == this.txoptimizations$snapshotChanges && tick == this.txoptimizations$snapshotTick)
            return;

        int count = this.particles.size();

        if (this.txoptimizations$positions.length < count * AXES) {
            int capacity = Math.min(Math.max(count, this.txoptimizations$positions.length / AXES * 2), MAX_PARTICLES);

            this.txoptimizations$positions = new double[capacity * AXES];
            this.txoptimizations$previous  = new double[capacity * AXES];
            this.txoptimizations$floats    = new float[capacity * ParticleSnapshot.FLOATS];
            this.txoptimizations$ints      = new int[capacity * ParticleSnapshot.INTS];
            this.txoptimizations$refs      = new Object[capacity * ParticleSnapshot.REFS];
            this.txoptimizations$order     = new Object[capacity];
            this.txoptimizations$bounds    = new double[(capacity + BLOCK - 1) / BLOCK * BOUNDS];
        }

        double[] positions = this.txoptimizations$positions;
        double[] bounds    = this.txoptimizations$bounds;
        Object[] order     = this.txoptimizations$order;
        int      index     = 0;

        Arrays.fill(order, count, order.length, null);
        ParticleLightShare.begin();

        try {
            for (SingleQuadParticle particle : this.particles) {
                positions[index * AXES]     = particle.x;
                positions[index * AXES + 1] = particle.y;
                positions[index * AXES + 2] = particle.z;
                order[index]                = particle;

                txoptimizations$include(bounds, (index >> BLOCK_SHIFT) * BOUNDS, (index & BLOCK - 1) == 0, particle.x, particle.y, particle.z);

                ((ParticleSnapshot) particle).txoptimizations$snapshot(this.txoptimizations$floats, index * ParticleSnapshot.FLOATS, this.txoptimizations$ints, index * ParticleSnapshot.INTS, this.txoptimizations$refs, index * ParticleSnapshot.REFS, this.txoptimizations$previous, index * AXES);
                index ++;
            }
        } finally {
            ParticleLightShare.end();
        }

        this.txoptimizations$count           = index;
        this.txoptimizations$snapshotChanges = changes;
        this.txoptimizations$snapshotTick    = tick;
    }

    @Unique
    private static void txoptimizations$include(double[] bounds, int at, boolean first, double x, double y, double z) {
        bounds[at]     = first ? x : Math.min(bounds[at], x);
        bounds[at + 1] = first ? y : Math.min(bounds[at + 1], y);
        bounds[at + 2] = first ? z : Math.min(bounds[at + 2], z);
        bounds[at + 3] = first ? x : Math.max(bounds[at + 3], x);
        bounds[at + 4] = first ? y : Math.max(bounds[at + 4], y);
        bounds[at + 5] = first ? z : Math.max(bounds[at + 5], z);
    }

    @Unique
    private void txoptimizations$extract(SingleQuadParticle particle, int index, Camera camera, float partialTickTime) {
        try {
            int flags = this.txoptimizations$ints[index * ParticleSnapshot.INTS + ParticleSnapshot.FLAGS];

            if ((flags & ParticleSnapshot.STORED) == 0) {
                particle.extract(this.particleTypeRenderState, camera, partialTickTime);
                return;
            }

            this.txoptimizations$extractStored(particle, index, flags, camera, partialTickTime);
        } catch (Throwable throwable) {
            CrashReport         report   = CrashReport.forThrowable(throwable, "Rendering Particle");
            CrashReportCategory category = report.addCategory("Particle being rendered");

            category.setDetail("Particle", particle::toString);
            category.setDetail("Particle Type", this.particleType::toString);

            throw new ReportedException(report);
        }
    }

    @Unique
    private void txoptimizations$extractStored(SingleQuadParticle particle, int index, int flags, Camera camera, float partialTickTime) {
        float[]     floats   = this.txoptimizations$floats;
        int[]       ints     = this.txoptimizations$ints;
        Object[]    refs     = this.txoptimizations$refs;
        int         floatAt  = index * ParticleSnapshot.FLOATS;
        int         intAt    = index * ParticleSnapshot.INTS;
        int         refAt    = index * ParticleSnapshot.REFS;
        int         axisAt   = index * AXES;
        Quaternionf rotation = ROTATION.identity();
        float       roll     = floats[floatAt + ParticleSnapshot.ROLL];

        ((SingleQuadParticle.FacingCameraMode) refs[refAt + ParticleSnapshot.FACING]).setRotation(rotation, camera, partialTickTime);

        if (roll != 0.0F)
            ParticleSnapshot.rotateRoll(rotation, Mth.lerp(partialTickTime, floats[floatAt + ParticleSnapshot.O_ROLL], roll));

        Vec3     pos       = camera.position();
        double[] positions = this.txoptimizations$positions;
        double[] previous  = this.txoptimizations$previous;
        float    x         = (float) (Mth.lerp(partialTickTime, previous[axisAt], positions[axisAt]) - pos.x());
        float    y         = (float) (Mth.lerp(partialTickTime, previous[axisAt + 1], positions[axisAt + 1]) - pos.y());
        float    z         = (float) (Mth.lerp(partialTickTime, previous[axisAt + 2], positions[axisAt + 2]) - pos.z());
        float    size      = (flags & ParticleSnapshot.OWN_SIZE) == 0 ? floats[floatAt + ParticleSnapshot.SIZE] : particle.getQuadSize(partialTickTime);
        int      light     = (flags & ParticleSnapshot.OWN_LIGHT) == 0 ? ints[intAt + ParticleSnapshot.LIGHT] : ((ParticleSnapshot) particle).txoptimizations$lightCoords(partialTickTime);

        this.particleTypeRenderState.add((SingleQuadParticle.Layer) refs[refAt + ParticleSnapshot.LAYER], x, y, z, rotation.x, rotation.y, rotation.z, rotation.w, size, floats[floatAt + ParticleSnapshot.U0], floats[floatAt + ParticleSnapshot.U1], floats[floatAt + ParticleSnapshot.V0], floats[floatAt + ParticleSnapshot.V1], ints[intAt + ParticleSnapshot.COLOR], light);
    }
}
