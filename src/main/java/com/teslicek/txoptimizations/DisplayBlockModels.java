package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class DisplayBlockModels {

    private static final BlockModelRenderState NOT_PLAIN = new BlockModelRenderState();

    private static final Map<BlockDisplayContext, Map<BlockState, BlockModelRenderState>> PREPARED = new IdentityHashMap<>();

    private static @Nullable BlockModelSet modelSet;
    private static boolean                 preparing;

    private DisplayBlockModels() {
    }

    public static boolean isPreparing() {
        return preparing;
    }

    public static @Nullable BlockModelRenderState prepared(BlockModelResolver resolver, BlockState blockState, BlockDisplayContext displayContext) {
        RenderSystem.assertOnRenderThread();

        BlockModelSet current = resolver.modelManager.getBlockModelSet();

        if (current != modelSet) {
            modelSet = current;
            PREPARED.clear();
        }

        Map<BlockState, BlockModelRenderState> states   = PREPARED.computeIfAbsent(displayContext, context -> new IdentityHashMap<>());
        BlockModelRenderState                  prepared = states.get(blockState);

        if (prepared == null) {
            prepared = prepare(resolver, current, blockState, displayContext);
            states.put(blockState, prepared);
        }

        return prepared == NOT_PLAIN ? null : prepared;
    }

    private static BlockModelRenderState prepare(BlockModelResolver resolver, BlockModelSet modelSet, BlockState blockState, BlockDisplayContext displayContext) {
        if (!PlainBlockModels.isPlain(modelSet.get(blockState)))
            return NOT_PLAIN;

        BlockModelRenderState state = new BlockModelRenderState();

        preparing = true;

        try {
            resolver.update(state, blockState, displayContext);
        } finally {
            preparing = false;
        }

        return ((BlockModelCopy) state).txoptimizations$hasAttachedData() ? NOT_PLAIN : state;
    }
}
