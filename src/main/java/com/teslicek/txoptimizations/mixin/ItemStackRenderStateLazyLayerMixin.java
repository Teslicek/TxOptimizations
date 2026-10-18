package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Arrays;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateLazyLayerMixin {

    @Shadow
    private int activeLayerCount;

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/item/ItemStackRenderState;)Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;"))
    private ItemStackRenderState.LayerRenderState txoptimizations$deferFirstLayer(ItemStackRenderState owner, Operation<ItemStackRenderState.LayerRenderState> original) {
        return null;
    }

    @Overwrite
    public void ensureCapacity(int requestedCount) {
        int requiredNewCapacity = this.activeLayerCount + requestedCount;

        if (requiredNewCapacity > this.layers.length)
            this.layers = Arrays.copyOf(this.layers, requiredNewCapacity);

        for (int i = this.activeLayerCount; i < requiredNewCapacity; i ++)
            if (this.layers[i] == null)
                this.layers[i] = ((ItemStackRenderState) (Object) this).new LayerRenderState();
    }

    @Overwrite
    public boolean usesBlockLight() {
        ItemStackRenderState.LayerRenderState firstLayer = this.layers[0];

        return firstLayer != null && ((ItemLayerRenderStateAccessor) firstLayer).txoptimizations$usesBlockLight();
    }
}
