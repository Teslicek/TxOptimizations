package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlobalSettingsUniform.class, priority = 999)
public abstract class GlobalSettingsUniformRingMixin {

    @Unique
    private final MappableRingBuffer txoptimizations$ring = new MappableRingBuffer(() -> "Global Settings UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, GlobalSettingsUniform.UBO_SIZE);

    @Overwrite
    public void update(int width, int height, double glintAlpha, long gameTime, float worldPartialTicks, int menuBlurRadius, Vec3 cameraPos, boolean useRgss) {
        int cameraX = Mth.floor(cameraPos.x);
        int cameraY = Mth.floor(cameraPos.y);
        int cameraZ = Mth.floor(cameraPos.z);

        this.txoptimizations$ring.rotate();

        GpuBuffer target = this.txoptimizations$ring.currentBuffer();

        try (GpuBufferSlice.MappedView view = target.map(false, true)) {
            Std140Builder.intoBuffer(view.data())
                .putIVec3(cameraX, cameraY, cameraZ)
                .putFloat((float) glintAlpha)
                .putVec3((float) (cameraX - cameraPos.x), (float) (cameraY - cameraPos.y), (float) (cameraZ - cameraPos.z))
                .putFloat(((float) (gameTime % 24000L) + worldPartialTicks) / 24000.0F)
                .putVec2(width, height)
                .putInt(menuBlurRadius)
                .putInt(useRgss ? 1 : 0);
        }

        RenderSystem.setGlobalSettingsUniform(target);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void txoptimizations$closeRing(CallbackInfo ci) {
        this.txoptimizations$ring.close();
    }
}
