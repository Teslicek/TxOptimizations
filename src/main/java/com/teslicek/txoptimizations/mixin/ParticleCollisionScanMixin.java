package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockCollisionScan;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class ParticleCollisionScanMixin {

    @Shadow
    @Final
    private static double MAXIMUM_COLLISION_VELOCITY_SQUARED;

    @Shadow
    @Final
    protected ClientLevel level;

    @Shadow
    public double y;

    @Shadow
    protected double xd;

    @Shadow
    protected double zd;

    @Shadow
    protected boolean onGround;

    @Shadow
    protected boolean hasPhysics;

    @Shadow
    private boolean stoppedByCollision;

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract void setBoundingBox(AABB box);

    @Shadow
    protected abstract void setLocationFromBoundingbox();

    @Overwrite
    public void move(double xa, double ya, double za) {
        if (this.stoppedByCollision)
            return;

        double originalXa = xa;
        double originalYa = ya;
        double originalZa = za;

        if (this.hasPhysics && (xa != 0.0 || ya != 0.0 || za != 0.0) && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            AABB box = this.getBoundingBox();

            if (!BlockCollisionScan.cannotCollide(this.level, this.y, box, xa, ya, za)) {
                Vec3 movement = Entity.collideBoundingBox(CollisionContext.positionContext(this.y), new Vec3(xa, ya, za), box, this.level, List.of());

                xa = movement.x;
                ya = movement.y;
                za = movement.z;
            }
        }

        if (xa != 0.0 || ya != 0.0 || za != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
            this.setLocationFromBoundingbox();
        }

        if (Math.abs(originalYa) >= 1.0E-5F && Math.abs(ya) < 1.0E-5F)
            this.stoppedByCollision = true;

        this.onGround = originalYa != ya && originalYa < 0.0;

        if (originalXa != xa)
            this.xd = 0.0;

        if (originalZa != za)
            this.zd = 0.0;
    }
}
