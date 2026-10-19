package com.teslicek.txoptimizations.mixin;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SectionRenderDispatcher.class, priority = 999)
public abstract class SectionRenderDispatcherCameraMixin {

    @Shadow
    @Final
    private AtomicReference<Vec3> cameraPosition;

    @Overwrite
    public void setCameraPosition(Vec3 cameraPosition) {
        if (cameraPosition.equals(this.cameraPosition.get()))
            return;

        this.cameraPosition.set(cameraPosition);
    }
}
