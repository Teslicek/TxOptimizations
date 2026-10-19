package com.teslicek.txoptimizations;

import java.util.Arrays;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;

public final class FrameModels {

    private static final int VARIANTS = 4;

    private static final BlockModelRenderState[] STATES      = new BlockModelRenderState[VARIANTS];
    private static final FrameModelSubmission[]  SUBMISSIONS = new FrameModelSubmission[VARIANTS];
    private static final boolean[]               PREPARED    = new boolean[VARIANTS];

    private static BlockModelSet modelSet;

    private FrameModels() {
    }

    public static BlockModelRenderState prepared(BlockModelResolver resolver, boolean glowing, boolean map) {
        BlockModelSet current = resolver.modelManager.getBlockModelSet();

        if (current != modelSet) {
            modelSet = current;
            Arrays.fill(STATES, null);
            Arrays.fill(SUBMISSIONS, null);
            Arrays.fill(PREPARED, false);
        }

        int variant = (glowing ? 2 : 0) | (map ? 1 : 0);

        if (!PREPARED[variant]) {
            STATES[variant]      = prepare(resolver, current, glowing, map);
            SUBMISSIONS[variant] = STATES[variant] == null ? null : FrameModelSubmission.capture(STATES[variant]);
            PREPARED[variant]    = true;
        }

        return STATES[variant];
    }

    public static FrameModelSubmission submission(BlockModelRenderState state) {
        for (int variant = 0; variant < VARIANTS; variant ++) {
            if (STATES[variant] == state)
                return SUBMISSIONS[variant];
        }

        return null;
    }

    private static BlockModelRenderState prepare(BlockModelResolver resolver, BlockModelSet modelSet, boolean glowing, boolean map) {
        if (!isPlain(modelSet.get(BlockStateDefinitions.getItemFrameFakeState(glowing, map))))
            return null;

        BlockModelRenderState state = new BlockModelRenderState();

        resolver.updateForItemFrame(state, glowing, map);

        return state;
    }

    private static boolean isPlain(BlockModel model) {
        if (model.getClass() != BlockStateModelWrapper.class)
            return false;

        BlockStateModelWrapper wrapper = (BlockStateModelWrapper) model;

        if (!wrapper.tints.isEmpty() || wrapper.model.getClass() != SingleVariant.class)
            return false;

        return ((SingleVariant) wrapper.model).model instanceof SimpleModelWrapper part && part.quads().getClass() == QuadCollection.class;
    }
}
