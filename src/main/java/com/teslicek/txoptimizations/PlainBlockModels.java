package com.teslicek.txoptimizations;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.util.random.Weighted;

public final class PlainBlockModels {

    private static final String VANILLA_TINTS = "net.minecraft.client.color.block.BlockTintSources$";

    private PlainBlockModels() {
    }

    public static boolean isPlain(BlockModel model) {
        if (model.getClass() != BlockStateModelWrapper.class)
            return false;

        BlockStateModelWrapper wrapper = (BlockStateModelWrapper) model;

        for (BlockTintSource tint : wrapper.tints) {
            if (!tint.getClass().getName().startsWith(VANILLA_TINTS))
                return false;
        }

        return isPlain(wrapper.model);
    }

    private static boolean isPlain(BlockStateModel model) {
        if (model.getClass() == SingleVariant.class)
            return ((SingleVariant) model).model instanceof SimpleModelWrapper part && part.getClass() == SimpleModelWrapper.class && part.quads().getClass() == QuadCollection.class;

        if (model.getClass() != WeightedVariants.class)
            return false;

        for (Weighted<BlockStateModel> variant : ((WeightedVariants) model).list.unwrap()) {
            if (!isPlain(variant.value()))
                return false;
        }

        return true;
    }
}
