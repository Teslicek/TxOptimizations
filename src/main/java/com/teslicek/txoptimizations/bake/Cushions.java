package com.teslicek.txoptimizations.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.CushionRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Cushions {

    private static final Map<SectionPos, Map<Integer, Snapshot>> SECTIONS = new ConcurrentHashMap<>();
    private static final Map<ModelKey, BlockStateModel>          MODELS   = new ConcurrentHashMap<>();

    private Cushions() {
    }

    public static void track(Entity entity) {
        if (!isTracked(entity))
            return;

        if (entity.isRemoved()) {
            untrack(entity);

            return;
        }

        Cushion cushion = (Cushion) entity;
        BlockPos pos    = entity.blockPosition();

        section(pos).put(entity.getId(), new Snapshot(entity.level(), entity.getId(), cushion.getPos(), cushion.position(), cushion.getRotationVector().y, cushion.getColor()));
        ((Bakeable) entity).txoptimizations$setPendingMode(RenderMode.TERRAIN);
        Baking.rebuild(pos);
    }

    public static void untrack(Entity entity) {
        if (!isTracked(entity))
            return;

        BlockPos               pos      = entity.blockPosition();
        SectionPos             section  = SectionPos.of(pos);
        Map<Integer, Snapshot> cushions = SECTIONS.get(section);

        if (cushions == null || cushions.remove(entity.getId()) == null)
            return;

        if (cushions.isEmpty())
            SECTIONS.remove(section, cushions);

        ChunkTasks.add(section, () -> ((Bakeable) entity).txoptimizations$setRenderMode(RenderMode.ENTITY));
        Baking.rebuild(pos);
    }

    public static void forEachMeshed(SectionPos section, MeshConsumer consumer) {
        Map<Integer, Snapshot> cushions = SECTIONS.get(section);

        if (cushions == null)
            return;

        Level level = Minecraft.getInstance().level;

        for (Snapshot snapshot : cushions.values()) {
            if (snapshot.level() != level) {
                cushions.remove(snapshot.id());

                continue;
            }

            ChunkTasks.add(section, () -> {
                Entity entity = Minecraft.getInstance().level.getEntity(snapshot.id());

                if (entity != null)
                    ((Bakeable) entity).txoptimizations$setRenderMode(RenderMode.TERRAIN);
            });

            consumer.accept(snapshot.blockPos(), model(snapshot));
        }
    }

    public static boolean hasCushions(SectionPos section) {
        Map<Integer, Snapshot> cushions = SECTIONS.get(section);

        return cushions != null && !cushions.isEmpty();
    }

    public static void clearModels() {
        MODELS.clear();
    }

    private static boolean isTracked(Entity entity) {
        return entity instanceof Bakeable bakeable && bakeable.txoptimizations$isBakeSupported() && entity.level().isClientSide();
    }

    private static Map<Integer, Snapshot> section(BlockPos pos) {
        return SECTIONS.computeIfAbsent(SectionPos.of(pos), _ -> new ConcurrentHashMap<>());
    }

    private static BlockStateModel model(Snapshot snapshot) {
        Vec3     offset = snapshot.position().subtract(Vec3.atLowerCornerOf(snapshot.blockPos()));
        ModelKey key    = new ModelKey(offset, snapshot.yRot(), snapshot.color());

        return MODELS.computeIfAbsent(key, _ -> {
            PoseStack poseStack = new PoseStack();
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.rotate(Axis.YP.rotationDegrees(snapshot.yRot()));
            poseStack.rotate(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0, -0.25, 0.0);

            return new BakedBlockModel(List.of(BakedBlockModel.bake(ModelLayers.CUSHION, BakedKind.blockAtlasId(CushionRenderer.TEXTURES_BY_COLOR.get(snapshot.color())), poseStack, false, null, null)), null);
        });
    }

    public interface MeshConsumer {

        void accept(BlockPos pos, BlockStateModel model);
    }

    private record Snapshot(Level level, int id, BlockPos blockPos, Vec3 position, float yRot, DyeColor color) {
    }

    private record ModelKey(Vec3 offset, float yRot, DyeColor color) {
    }
}
