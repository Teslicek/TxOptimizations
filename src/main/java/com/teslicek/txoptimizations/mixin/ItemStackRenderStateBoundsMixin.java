package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemBoundsCache;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.impl.client.renderer.LayerRenderStateExtension;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateBoundsMixin {

    @Shadow
    ItemDisplayContext displayContext;

    @Shadow
    private int activeLayerCount;

    @Shadow
    private AABB cachedModelBoundingBox;

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @Shadow
    public abstract void visitExtents(Consumer<Vector3fc> output);

    @Overwrite
    public AABB getModelBoundingBox() {
        if (this.cachedModelBoundingBox != null)
            return this.cachedModelBoundingBox;

        this.cachedModelBoundingBox = this.activeLayerCount == 1 && txoptimizations$hasNoMeshQuads(this.layers[0]) ? this.txoptimizations$cachedBounds((ItemLayerRenderStateAccessor) this.layers[0]) : this.txoptimizations$computeBounds();

        return this.cachedModelBoundingBox;
    }

    @Unique
    private AABB txoptimizations$cachedBounds(ItemLayerRenderStateAccessor layer) {
        boolean leftHand = this.displayContext.leftHand();
        AABB    cached   = ItemBoundsCache.find(layer.txoptimizations$extents(), layer.txoptimizations$itemTransform(), leftHand, layer.txoptimizations$localTransform());

        if (cached != null)
            return cached;

        return ItemBoundsCache.store(this.txoptimizations$computeBounds(), layer.txoptimizations$extents(), layer.txoptimizations$itemTransform(), leftHand, layer.txoptimizations$localTransform());
    }

    @Unique
    private AABB txoptimizations$computeBounds() {
        AABB.Builder collector = new AABB.Builder();

        this.visitExtents(collector::include);

        return collector.isDefined() ? collector.build() : AABB.ofSize(Vec3.ZERO, 0.0, 0.0, 0.0);
    }

    @Unique
    private static boolean txoptimizations$hasNoMeshQuads(ItemStackRenderState.LayerRenderState layer) {
        MutableMesh mesh = ((LayerRenderStateExtension) layer).fabric_getMutableMesh();

        return mesh == null || mesh.size() == 0;
    }
}
