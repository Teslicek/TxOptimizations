package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Arrays;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ItemStackRenderState.class, priority = 999)
public abstract class ItemStackRenderStateLazyLayerMixin {

    @Unique
    private static final ItemStackRenderState.LayerRenderState[] NO_LAYERS = new ItemStackRenderState.LayerRenderState[0];

    @Shadow
    private int activeLayerCount;

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/item/ItemStackRenderState;)Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;"))
    private ItemStackRenderState.LayerRenderState txoptimizations$deferFirstLayer(ItemStackRenderState owner, Operation<ItemStackRenderState.LayerRenderState> original) {
        return null;
    }

    @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;layers:[Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;", opcode = Opcodes.PUTFIELD))
    private void txoptimizations$shareNoLayers(ItemStackRenderState owner, ItemStackRenderState.LayerRenderState[] deferred, Operation<Void> original) {
        if ((Object) owner != this || deferred.length != 1 || deferred[0] != null)
            throw new IllegalStateException("Expected the item render state to store its deferred first layer, got " + Arrays.toString(deferred));

        this.layers = NO_LAYERS;
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
        return this.layers.length != 0 && ((ItemLayerRenderStateAccessor) this.layers[0]).txoptimizations$usesBlockLight();
    }
}
