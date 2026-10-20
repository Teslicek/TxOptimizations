package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.texture.DynamicAtlasTree;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DynamicAtlasTree.class, priority = 999)
public abstract class DynamicAtlasTreeFreeSpaceMixin {

    @Unique
    private static final int NIBBLE = 4;

    @Unique
    private static final int NIBBLE_MASK = (1 << NIBBLE) - 1;

    @Shadow
    @Final
    private int x;

    @Shadow
    @Final
    private int y;

    @Shadow
    @Final
    private int width;

    @Shadow
    @Final
    private int height;

    @Shadow
    private @Nullable DynamicAtlasTree left;

    @Shadow
    private @Nullable DynamicAtlasTree right;

    @Shadow
    private boolean occupied;

    @Unique
    private byte txoptimizations$freeBits;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$startFree(int x, int y, int width, int height, CallbackInfo ci) {
        this.txoptimizations$refresh();
    }

    @Overwrite
    private @Nullable DynamicAtlasTree insertInner(int slotWidth, int slotHeight, int spacing) {
        int free = this.txoptimizations$freeBits;

        if (slotWidth >= 1 << (free & NIBBLE_MASK) || slotHeight >= 1 << (free >>> NIBBLE & NIBBLE_MASK))
            return null;

        DynamicAtlasTree inserted;

        if (this.left != null && this.right != null) {
            inserted = txoptimizations$of(this.left).insertInner(slotWidth, slotHeight, spacing);

            if (inserted == null)
                inserted = txoptimizations$of(this.right).insertInner(slotWidth, slotHeight, spacing);
        } else {
            if (this.occupied || slotWidth > this.width || slotHeight > this.height)
                return null;

            if (slotWidth == this.width && slotHeight == this.height) {
                this.occupied = true;
                this.txoptimizations$refresh();

                return (DynamicAtlasTree) (Object) this;
            }

            if (this.width - slotWidth > this.height - slotHeight) {
                this.left  = new DynamicAtlasTree(this.x, this.y, slotWidth, this.height);
                this.right = new DynamicAtlasTree(this.x + slotWidth + spacing, this.y, this.width - slotWidth - spacing, this.height);
            } else {
                this.left  = new DynamicAtlasTree(this.x, this.y, this.width, slotHeight);
                this.right = new DynamicAtlasTree(this.x, this.y + slotHeight + spacing, this.width, this.height - slotHeight - spacing);
            }

            inserted = txoptimizations$of(this.left).insertInner(slotWidth, slotHeight, spacing);
        }

        this.txoptimizations$refresh();

        return inserted;
    }

    @Unique
    private void txoptimizations$refresh() {
        if (this.left != null && this.right != null) {
            int left  = txoptimizations$of(this.left).txoptimizations$freeBits;
            int right = txoptimizations$of(this.right).txoptimizations$freeBits;

            this.txoptimizations$freeBits = (byte) (Math.max(left & NIBBLE_MASK, right & NIBBLE_MASK) | Math.max(left >>> NIBBLE & NIBBLE_MASK, right >>> NIBBLE & NIBBLE_MASK) << NIBBLE);

            return;
        }

        this.txoptimizations$freeBits = this.occupied ? 0 : (byte) (txoptimizations$bits(this.width) | txoptimizations$bits(this.height) << NIBBLE);
    }

    @Unique
    private static int txoptimizations$bits(int size) {
        int bits = Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(size, 0));

        if (bits > NIBBLE_MASK)
            throw new IllegalStateException("An atlas tree side of " + size + " pixels does not fit the " + NIBBLE + " bit free space bound");

        return bits;
    }

    @Unique
    private static DynamicAtlasTreeFreeSpaceMixin txoptimizations$of(DynamicAtlasTree tree) {
        return (DynamicAtlasTreeFreeSpaceMixin) (Object) tree;
    }
}
