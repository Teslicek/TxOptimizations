package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teslicek.txoptimizations.DisplayCulling;
import com.teslicek.txoptimizations.ItemBoundsCache;
import com.teslicek.txoptimizations.ItemCullingBounds;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemStackRenderState.class, priority = 999)
public abstract class ItemStackRenderStateEarlyMixin implements ItemCullingBounds {

    @Shadow
    ItemDisplayContext displayContext;

    @Shadow
    private int activeLayerCount;

    @Shadow
    private AABB cachedModelBoundingBox;

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @Unique
    private ItemBoundsCache.Entry txoptimizations$lastBounds;

    @Unique
    private AABB txoptimizations$cullingFor;

    @Unique
    private AABB txoptimizations$culling;

    @Shadow
    public abstract void visitExtents(Consumer<Vector3fc> output);

    @Overwrite
    public AABB getModelBoundingBox() {
        if (this.cachedModelBoundingBox != null)
            return this.cachedModelBoundingBox;

        this.cachedModelBoundingBox = this.activeLayerCount == 1 ? this.txoptimizations$cachedBounds((ItemLayerRenderStateAccessor) this.layers[0]) : this.txoptimizations$computeBounds();

        return this.cachedModelBoundingBox;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void txoptimizations$forgetCulling(CallbackInfo ci) {
        this.txoptimizations$cullingFor = null;
        this.txoptimizations$culling    = null;
    }

    @Override
    public AABB txoptimizations$cullingBounds() {
        AABB model = this.cachedModelBoundingBox;

        if (model != null && model == this.txoptimizations$cullingFor)
            return this.txoptimizations$culling;

        AABB bounds = this.txoptimizations$cullable() ? this.getModelBoundingBox() : null;

        this.txoptimizations$cullingFor = this.getModelBoundingBox();
        this.txoptimizations$culling    = bounds;

        return bounds;
    }

    @Unique
    private boolean txoptimizations$cullable() {
        for (int index = 0; index < this.activeLayerCount; index ++) {
            ItemLayerRenderStateAccessor layer = (ItemLayerRenderStateAccessor) this.layers[index];

            if (layer.txoptimizations$extents().get().length == 0 || layer.txoptimizations$specialRenderer() != null && !DisplayCulling.isVanilla(layer.txoptimizations$specialRenderer()))
                return false;
        }

        return true;
    }

    @Unique
    private AABB txoptimizations$cachedBounds(ItemLayerRenderStateAccessor layer) {
        boolean               leftHand       = this.displayContext.leftHand();
        Supplier<Vector3fc[]> extents        = layer.txoptimizations$extents();
        ItemTransform         itemTransform  = layer.txoptimizations$itemTransform();
        Matrix4f              localTransform = layer.txoptimizations$localTransform();
        ItemBoundsCache.Entry entry          = this.txoptimizations$lastBounds;

        if (entry != null && entry.matches(extents, itemTransform, leftHand, localTransform))
            return entry.bounds();

        entry = ItemBoundsCache.find(extents, itemTransform, leftHand, localTransform);

        if (entry == null)
            entry = ItemBoundsCache.store(this.txoptimizations$computeBounds(), extents, itemTransform, leftHand, localTransform);

        this.txoptimizations$lastBounds = entry;

        return entry.bounds();
    }

    @Unique
    private AABB txoptimizations$computeBounds() {
        AABB.Builder collector = new AABB.Builder();

        this.visitExtents(collector::include);

        return collector.isDefined() ? collector.build() : AABB.ofSize(Vec3.ZERO, 0.0, 0.0, 0.0);
    }

    @Unique
    private static final ItemStackRenderState.LayerRenderState[] NO_LAYERS = new ItemStackRenderState.LayerRenderState[0];

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
