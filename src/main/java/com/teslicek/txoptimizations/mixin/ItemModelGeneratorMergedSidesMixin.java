package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemSideFaces;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ItemModelGenerator.class, priority = 999)
public abstract class ItemModelGeneratorMergedSidesMixin {

    @Overwrite
    private static void bakeSideFaces(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo) {
        ItemSideFaces.bake(builder, interner, modelState, materialInfo);
    }
}
