package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.SectionSubset;
import com.teslicek.txoptimizations.cull.EntityCulling;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorBlockEntitySectionsMixin {

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;iterator()Lit/unimi/dsi/fastutil/objects/ObjectListIterator;"))
    private ObjectListIterator<SectionRenderDispatcher.RenderSection> txoptimizations$blockEntitySections(ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections) {
        return SectionSubset.BLOCK_ENTITIES.iterator(visibleSections);
    }

    @ModifyExpressionValue(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getRenderableBlockEntities()Ljava/util/List;"))
    private List<BlockEntity> txoptimizations$recordBlockEntitySection(List<BlockEntity> blockEntities, @Local SectionRenderDispatcher.RenderSection section) {
        SectionSubset.BLOCK_ENTITIES.note(section, !blockEntities.isEmpty());

        if (!blockEntities.isEmpty())
            EntityCulling.enterBlockEntitySection(section);

        return blockEntities;
    }

    @Redirect(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
    private Object txoptimizations$skipEmptyDestruction(Long2ObjectMap<?> destruction, long pos) {
        if (destruction.isEmpty())
            return null;

        return destruction.get(pos);
    }

    @Inject(method = "extractVisibleBlockEntities", at = @At("RETURN"))
    private void txoptimizations$leaveBlockEntitySections(CallbackInfo ci) {
        EntityCulling.leaveBlockEntitySections();
    }
}
