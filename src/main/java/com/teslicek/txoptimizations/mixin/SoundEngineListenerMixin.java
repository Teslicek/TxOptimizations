package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundEngine.class, priority = 999)
public abstract class SoundEngineListenerMixin {

    @Unique
    private static final long MIN_UPDATE_INTERVAL_NANOS = 5_000_000L;

    @Shadow
    private boolean loaded;

    @Shadow
    @Final
    private Listener listener;

    @Shadow
    @Final
    private SoundEngineExecutor executor;

    @Unique
    private ListenerTransform txoptimizations$sentTransform;

    @Unique
    private long txoptimizations$sentNanos;

    @Overwrite
    public void updateSource(Camera camera) {
        if (!this.loaded || !camera.isInitialized())
            return;

        Vec3              position = camera.position();
        Vector3fc         forward  = camera.forwardVector();
        Vector3fc         up       = camera.upVector();
        ListenerTransform sent     = this.txoptimizations$sentTransform;

        if (sent != null && sent.position().equals(position) && txoptimizations$same(sent.forward(), forward) && txoptimizations$same(sent.up(), up))
            return;

        long now = System.nanoTime();

        if (sent != null && now - this.txoptimizations$sentNanos < MIN_UPDATE_INTERVAL_NANOS)
            return;

        ListenerTransform transform = new ListenerTransform(position, new Vec3(forward), new Vec3(up));

        this.executor.execute(() -> this.listener.setTransform(transform));
        this.txoptimizations$sentTransform = transform;
        this.txoptimizations$sentNanos     = now;
    }

    @Inject(method = "loadLibrary", at = @At("TAIL"))
    private void txoptimizations$forgetSentListener(CallbackInfo ci) {
        this.txoptimizations$sentTransform = null;
    }

    @Unique
    private static boolean txoptimizations$same(Vec3 sent, Vector3fc vector) {
        return Double.compare(sent.x, vector.x()) == 0 && Double.compare(sent.y, vector.y()) == 0 && Double.compare(sent.z, vector.z()) == 0;
    }
}
