package com.teslicek.txoptimizations.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.world.level.block.state.BlockState;

public final class BakedModels {

    private static final Map<BlockState, BlockStateModel> MODELS = new ConcurrentHashMap<>();

    private static volatile boolean ready;

    private BakedModels() {
    }

    public static BlockStateModel withBlockEntity(BlockState state, BlockStateModel vanilla) {
        if (!ready || !state.hasBlockEntity())
            return vanilla;

        BakedKind kind = BakedKind.of(state);

        if (kind == null)
            return vanilla;

        BlockStateModel cached = MODELS.get(state);

        if (cached != null)
            return cached;

        if (!kind.canBake(state))
            return vanilla;

        return MODELS.computeIfAbsent(state, _ -> bake(state, kind, vanilla));
    }

    public static BlockStateModel vanilla(BlockState state) {
        BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

        return models.modelByState.getOrDefault(state, models.missingModel());
    }

    public static void reset() {
        MODELS.clear();
        Cushions.clearModels();
        ready = true;
    }

    private static BlockStateModel bake(BlockState state, BakedKind kind, BlockStateModel vanilla) {
        PoseStack poseStack = new PoseStack();
        kind.transform(state, poseStack);

        BlockStateModelPart body = BakedBlockModel.bake(kind.layer(state), kind.texture(state), poseStack, kind.hasAmbientOcclusion(), state, vanilla.particleMaterial());

        if (kind != BakedKind.DECORATED_POT)
            return new BakedBlockModel(List.of(body), vanilla.particleMaterial(), vanilla);

        BlockStateModelPart sides = BakedBlockModel.bake(ModelLayers.DECORATED_POT_SIDES, Sheets.DECORATED_POT_SIDE.texture(), poseStack, kind.hasAmbientOcclusion(), state, vanilla.particleMaterial());

        return new BakedBlockModel(List.of(body, sides), vanilla.particleMaterial());
    }
}
