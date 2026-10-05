package com.teslicek.txoptimizations.mixin;

import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRenderOffsetMixin {

    @Unique
    private static final Vec3[] OFFSETS = txoptimizations$offsets();

    @Overwrite
    public Vec3 getRenderOffset(ItemFrameRenderState state) {
        return OFFSETS[state.direction.ordinal()];
    }

    @Unique
    private static Vec3[] txoptimizations$offsets() {
        Direction[] directions = Direction.values();
        Vec3[]      offsets    = new Vec3[directions.length];

        for (Direction direction : directions)
            offsets[direction.ordinal()] = new Vec3(direction.getStepX() * 0.3F, -0.25, direction.getStepZ() * 0.3F);

        return offsets;
    }
}
