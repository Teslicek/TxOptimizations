package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.teslicek.txoptimizations.AnimatedItemAtlas;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererAnimatedItemAtlasMixin {

    @Unique
    private static final int MINIMUM_ANIMATED_ITEMS = 3;

    @Shadow
    @Final
    private GuiRenderState renderState;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow
    private int cachedGuiScale;

    @Unique
    private AnimatedItemAtlas txoptimizations$animatedAtlas;

    @Unique
    private boolean txoptimizations$animatedAtlasActive;

    @Unique
    private final Set<Object> txoptimizations$animatedItems = new ObjectOpenHashSet<>();

    @Unique
    private final Consumer<GuiItemRenderState> txoptimizations$collectAnimatedItem = itemRenderState -> {
        TrackingItemStackRenderState item = itemRenderState.itemStackRenderState();

        if (itemRenderState.oversizedItemBounds() == null && item.isAnimated())
            this.txoptimizations$animatedItems.add(item.getModelIdentity());
    };

    @Inject(method = "prepareItemElements", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepareItemAtlas(Ljava/util/Set;I)Lnet/minecraft/client/gui/render/GuiItemAtlas;"))
    private void txoptimizations$prepareAnimatedAtlas(CallbackInfo ci) {
        this.txoptimizations$animatedItems.clear();
        this.renderState.forEachItem(this.txoptimizations$collectAnimatedItem);
        this.txoptimizations$animatedAtlasActive = this.txoptimizations$fitAnimatedAtlas(this.txoptimizations$animatedItems, GuiRenderer.DEFAULT_ITEM_SIZE * this.cachedGuiScale);
    }

    @ModifyReceiver(method = "lambda$prepareItemElements$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiItemAtlas;getOrUpdate(Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;)Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;"))
    private GuiItemAtlas txoptimizations$routeAnimatedItems(GuiItemAtlas atlas, TrackingItemStackRenderState item) {
        if (!this.txoptimizations$animatedAtlasActive || !item.isAnimated())
            return atlas;

        return this.txoptimizations$animatedAtlas;
    }

    @Inject(method = "endFrame", at = @At("RETURN"))
    private void txoptimizations$endAnimatedAtlasFrame(CallbackInfo ci) {
        if (this.txoptimizations$animatedAtlas == null)
            return;

        this.txoptimizations$animatedAtlas.endFrame();
    }

    @Inject(method = {"invalidateItemAtlas", "close"}, at = @At("RETURN"))
    private void txoptimizations$releaseAnimatedAtlas(CallbackInfo ci) {
        this.txoptimizations$closeAnimatedAtlas();
    }

    @Unique
    private boolean txoptimizations$fitAnimatedAtlas(Set<Object> animatedItems, int slotTextureSize) {
        if (animatedItems.size() < MINIMUM_ANIMATED_ITEMS)
            return false;

        int textureSize = AnimatedItemAtlas.textureSizeFor(slotTextureSize, animatedItems.size());

        if (this.txoptimizations$animatedAtlas == null || textureSize > this.txoptimizations$animatedAtlas.textureSize() || textureSize < this.txoptimizations$animatedAtlas.textureSize() / 4) {
            this.txoptimizations$closeAnimatedAtlas();
            this.txoptimizations$animatedAtlas = new AnimatedItemAtlas(this.featureRenderDispatcher, textureSize, slotTextureSize);
        }

        return this.txoptimizations$animatedAtlas.tryPrepareFor(animatedItems);
    }

    @Unique
    private void txoptimizations$closeAnimatedAtlas() {
        if (this.txoptimizations$animatedAtlas == null)
            return;

        this.txoptimizations$animatedAtlas.close();
        this.txoptimizations$animatedAtlas = null;
    }
}
