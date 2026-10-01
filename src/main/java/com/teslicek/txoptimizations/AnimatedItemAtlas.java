package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.util.Mth;

public final class AnimatedItemAtlas extends GuiItemAtlas {

    private static final int MINIMUM_TEXTURE_SIZE = 128;

    public AnimatedItemAtlas(FeatureRenderDispatcher featureRenderDispatcher, int textureSize, int slotTextureSize) {
        super(featureRenderDispatcher, textureSize, slotTextureSize);
    }

    @Override
    public SlotView getOrUpdate(TrackingItemStackRenderState item) {
        if (!item.isAnimated())
            throw new IllegalArgumentException("Item " + item.getModelIdentity() + " is not animated");

        return super.getOrUpdate(item);
    }

    @Override
    public void endFrame() {
        if (!this.allocator.usedSlotByKey.isEmpty()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(this.texture, GuiRenderer.CLEAR_COLOR, this.depthTexture, 0.0);

            for (DynamicAtlasAllocator.Slot slot : this.allocator.usedSlotByKey.values())
                slot.fresh = true;
        }

        super.endFrame();
    }

    public static int textureSizeFor(int slotTextureSize, int slotCount) {
        int side = Mth.smallestSquareSide(slotCount + slotCount / 2);

        return Math.clamp(Mth.smallestEncompassingPowerOfTwo(side * slotTextureSize), MINIMUM_TEXTURE_SIZE, RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSizeForFormat(GpuFormat.RGBA8_UNORM));
    }
}
