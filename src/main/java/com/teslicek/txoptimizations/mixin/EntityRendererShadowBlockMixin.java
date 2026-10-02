package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ShadowBlockCache;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererShadowBlockMixin {

    @Overwrite
    private void extractShadowPiece(EntityRenderState state, Level level, float pow, BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        float                  powerAtDepth = pow - (float) (state.y - pos.getY()) * 0.5F;
        ShadowBlockCache.Block block        = ShadowBlockCache.get(level, pos, chunk);

        if (block == null)
            return;

        float alpha     = Mth.clamp(powerAtDepth * 0.5F * Lightmap.getBrightness(level.dimensionType(), block.brightness()), 0.0F, 1.0F);
        float relativeX = (float) (pos.getX() - state.x);
        float relativeY = (float) (pos.getY() - state.y);
        float relativeZ = (float) (pos.getZ() - state.z);

        state.shadowPieces.add(new EntityRenderState.ShadowPiece(relativeX, relativeY, relativeZ, block.shape(), alpha));
    }
}
