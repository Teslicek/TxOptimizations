package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionStateAccess;
import com.teslicek.txoptimizations.SectionStateWindow;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionStateWindowMixin implements SectionStateAccess {

    @Unique
    private SectionStateWindow txoptimizations$states;

    @Shadow
    private SectionCopy getSection(int sectionX, int sectionY, int sectionZ) {
        throw new AssertionError();
    }

    @Override
    public BlockState txoptimizations$directBlockState(BlockPos pos) {
        return this.getSection(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ())).getBlockState(pos);
    }

    @Override
    public void txoptimizations$windowStates(SectionStateWindow window) {
        this.txoptimizations$states = window;
    }

    @Overwrite
    public BlockState getBlockState(final BlockPos pos) {
        SectionStateWindow window = this.txoptimizations$states;

        if (window == null)
            return this.txoptimizations$directBlockState(pos);

        return window.get(this, pos);
    }
}
