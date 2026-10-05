package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class BlockEntityDistance {

    private static final ClassValue<Boolean> INHERITED = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("shouldRender", BlockEntity.class, Vec3.class).getDeclaringClass() == BlockEntityRenderer.class;
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Block entity renderer " + type.getName() + " has no shouldRender method", exception);
            }
        }
    };

    private BlockEntityDistance() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean shouldRender(BlockEntityRenderer renderer, BlockEntity blockEntity, Vec3 cameraPosition) {
        if (!INHERITED.get(renderer.getClass()))
            return renderer.shouldRender(blockEntity, cameraPosition);

        BlockPos pos      = blockEntity.getBlockPos();
        double   xd       = cameraPosition.x() - (pos.getX() + 0.5);
        double   yd       = cameraPosition.y() - (pos.getY() + 0.5);
        double   zd       = cameraPosition.z() - (pos.getZ() + 0.5);
        double   distance = renderer.getViewDistance();

        return xd * xd + yd * yd + zd * zd < distance * distance;
    }
}
