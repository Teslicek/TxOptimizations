package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.NearPlane.class)
public interface CameraNearPlaneAccessor {

    @Accessor("forward")
    Vec3 txoptimizations$forward();
}
