package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import com.teslicek.txoptimizations.PersistentRingBuffer;
import java.nio.ByteBuffer;
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

        if (target instanceof VulkanGpuBuffer.Direct) {
            txoptimizations$write(((PersistentRingBuffer) this.txoptimizations$ring).txoptimizations$mapCurrent().clear(), width, height, glintAlpha, gameTime, worldPartialTicks, menuBlurRadius, cameraPos, useRgss, cameraX, cameraY, cameraZ);
        } else {
            try (GpuBufferSlice.MappedView view = target.map(false, true)) {
                txoptimizations$write(view.data(), width, height, glintAlpha, gameTime, worldPartialTicks, menuBlurRadius, cameraPos, useRgss, cameraX, cameraY, cameraZ);
            }
        }

        RenderSystem.setGlobalSettingsUniform(target);
    }

    @Unique
    private static void txoptimizations$write(ByteBuffer data, int width, int height, double glintAlpha, long gameTime, float worldPartialTicks, int menuBlurRadius, Vec3 cameraPos, boolean useRgss, int cameraX, int cameraY, int cameraZ) {
        Std140Builder.intoBuffer(data)
            .putIVec3(cameraX, cameraY, cameraZ)
            .putFloat((float) glintAlpha)
            .putVec3((float) (cameraX - cameraPos.x), (float) (cameraY - cameraPos.y), (float) (cameraZ - cameraPos.z))
            .putFloat(((float) (gameTime % 24000L) + worldPartialTicks) / 24000.0F)
            .putVec2(width, height)
            .putInt(menuBlurRadius)
            .putInt(useRgss ? 1 : 0);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void txoptimizations$closeRing(CallbackInfo ci) {
        this.txoptimizations$ring.close();
    }
}
