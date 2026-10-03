package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.FrustumBoxTest;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustum.class)
public abstract class FrustumBoxTestMixin implements FrustumBoxTest {

    @Shadow
    @Final
    private FrustumIntersection intersection;

    @Shadow
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    @Overwrite
    public boolean isVisible(AABB box) {
        return this.txoptimizations$isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    @Override
    public boolean txoptimizations$isBoxVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.intersection.testAab((float) (minX - this.camX), (float) (minY - this.camY), (float) (minZ - this.camZ), (float) (maxX - this.camX), (float) (maxY - this.camY), (float) (maxZ - this.camZ));
    }
}
