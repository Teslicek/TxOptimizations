package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.SectionSubset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorBlockEntitySectionsMixin {

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<SectionRenderDispatcher.RenderSection> txoptimizations$blockEntitySections(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        return SectionSubset.BLOCK_ENTITIES.iterator(visibleSections);
    }

    @ModifyExpressionValue(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getRenderableBlockEntities()Ljava/util/List;"))
    private List<BlockEntity> txoptimizations$recordBlockEntitySection(List<BlockEntity> blockEntities, @Local SectionRenderDispatcher.RenderSection section) {
        SectionSubset.BLOCK_ENTITIES.note(section, !blockEntities.isEmpty());

        return blockEntities;
    }
}
