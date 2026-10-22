package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ParticleGroupChanges;
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

    @Shadow
    @Final
    private ParticleRenderType particleType;

    @Shadow
    @Final
    private QuadParticleRenderState particleTypeRenderState;

    @Unique
    private double[] txoptimizations$positions = new double[0];

    @Unique
    private int txoptimizations$positionsChanges = -1;

    protected QuadParticleGroupPositionsMixin(ParticleEngine engine) {
        super(engine);
    }

    @Overwrite
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        double[] positions = this.txoptimizations$positions();
        int      index     = 0;

        for (Object particle : this.particles) {
            if (frustum.pointInFrustum(positions[index], positions[index + 1], positions[index + 2]))
                this.txoptimizations$extract((SingleQuadParticle) particle, camera, partialTickTime);

            index += AXES;
        }

        return this.particleTypeRenderState;
    }

    @Unique
    private double[] txoptimizations$positions() {
        int changes = ((ParticleGroupChanges) (Object) this).txoptimizations$changes();

        if (changes == this.txoptimizations$positionsChanges)
            return this.txoptimizations$positions;

        int      count     = this.particles.size();
        double[] positions = this.txoptimizations$positions;

        if (positions.length < count * AXES)
            positions = new double[Math.min(Math.max(count, positions.length / AXES * 2), MAX_PARTICLES) * AXES];

        int index = 0;

        for (SingleQuadParticle particle : this.particles) {
            positions[index ++] = particle.x;
            positions[index ++] = particle.y;
            positions[index ++] = particle.z;
        }

        this.txoptimizations$positions        = positions;
        this.txoptimizations$positionsChanges = changes;

        return positions;
    }

    @Unique
    private void txoptimizations$extract(SingleQuadParticle particle, Camera camera, float partialTickTime) {
        try {
            particle.extract(this.particleTypeRenderState, camera, partialTickTime);
        } catch (Throwable throwable) {
            CrashReport         report   = CrashReport.forThrowable(throwable, "Rendering Particle");
            CrashReportCategory category = report.addCategory("Particle being rendered");

            category.setDetail("Particle", particle::toString);
            category.setDetail("Particle Type", this.particleType::toString);

            throw new ReportedException(report);
        }
    }
}
