package com.teslicek.txoptimizations.mixin;

import java.util.Optional;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ParticleGroup.class, priority = 999)
public abstract class ParticleGroupTickMixin<P extends Particle> {

    @Shadow
    @Final
    protected ParticleEngine engine;

    @Shadow
    @Final
    protected Queue<P> particles;

    @Shadow
    private void tickParticle(Particle particle) {
        throw new AssertionError();
    }

    @Overwrite
    public void tickParticles() {
        for (int remaining = this.particles.size(); remaining > 0; remaining --) {
            P particle = this.particles.remove();

            this.tickParticle(particle);

            if (particle.isAlive()) {
                this.particles.add(particle);
                continue;
            }

            Optional<ParticleLimit> limit = particle.getParticleLimit();

            if (limit.isPresent())
                this.engine.updateCount(limit.get(), -1);
        }
    }
}
