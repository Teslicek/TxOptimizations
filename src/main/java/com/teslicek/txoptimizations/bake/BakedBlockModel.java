package com.teslicek.txoptimizations.bake;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class BakedBlockModel implements BlockStateModel {

    private static final float CHEST_SEAM_LOW  = 8.9F / 16.0F;
    private static final float CHEST_SEAM_HIGH = 10.1F / 16.0F;
    private static final float SHULKER_CORNER  = 1.0F / 32.0F;
    private static final int   QUAD_VERTICES   = 4;

    private final List<BlockStateModelPart> parts;
    private final BlockStateModel[]         underlying;
    private final Material.Baked            particleMaterial;

    public BakedBlockModel(List<BlockStateModelPart> parts, Material.Baked particleMaterial, BlockStateModel... underlying) {
        this.parts            = parts;
        this.underlying       = underlying;
        this.particleMaterial = particleMaterial;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.addAll(this.parts);

        for (BlockStateModel model : this.underlying)
            model.collectParts(random, output);
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return 0;
    }

    public static BlockStateModelPart bake(ModelLayerLocation layer, Identifier texture, PoseStack poseStack, boolean ambientOcclusion, BlockState state, Material.Baked particleMaterial) {
        TextureAtlasSprite     sprite  = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(texture);
        Material.Baked         surface = new Material.Baked(sprite, false);
        QuadCollection.Builder quads   = new QuadCollection.Builder();
        ModelPart              root    = Minecraft.getInstance().getEntityModels().bakeLayer(layer);

        root.children.forEach((name, part) -> bakePart(quads, part, name, poseStack, sprite, surface, state));

        return new SimpleModelWrapper(quads.build(), ambientOcclusion, particleMaterial);
    }

    private static void bakePart(QuadCollection.Builder quads, ModelPart part, String name, PoseStack poseStack, TextureAtlasSprite sprite, Material.Baked surface, BlockState state) {
        boolean  skull  = state != null && state.getBlock() instanceof AbstractSkullBlock;
        Vector3f normal = new Vector3f();

        part.visit(poseStack, (pose, path, cubeIndex, cube) -> {
            Transparency           transparency = skull && path.isEmpty() ? Transparency.NONE : Transparency.TRANSPARENT;
            BakedQuad.MaterialInfo material     = BakedQuad.MaterialInfo.of(surface, transparency, -1, null, 0);

            for (ModelPart.Polygon polygon : cube.polygons) {
                if (polygon.vertices().length != QUAD_VERTICES)
                    continue;

                Vector3fc[] positions = new Vector3fc[QUAD_VERTICES];
                long[]      uvs       = new long[QUAD_VERTICES];

                for (int i = 0; i < QUAD_VERTICES; i ++) {
                    ModelPart.Vertex vertex = polygon.vertices()[i];
                    positions[i] = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                    uvs[i]       = UVPair.pack(sprite.getU(vertex.u()), sprite.getV(vertex.v()));
                }

                if (isHiddenFace(positions, state, name))
                    continue;

                polygon.normal().mul(pose.normal(), normal);

                Direction direction = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());

                quads.addUnculledFace(quad(positions, uvs, direction, material, null));

                if (skull)
                    quads.addUnculledFace(quad(positions, uvs, direction.getOpposite(), material, new int[] {0, 3, 2, 1}));
            }
        });
    }

    private static BakedQuad quad(Vector3fc[] positions, long[] uvs, Direction direction, BakedQuad.MaterialInfo material, int[] fallbackOrder) {
        Vector3fc[] wound    = positions.clone();
        long[]      woundUvs = uvs.clone();

        try {
            FaceBakery.recalculateWinding(wound, woundUvs, direction);
        } catch (IllegalStateException notAxisAligned) {
            wound    = reorder(positions, fallbackOrder);
            woundUvs = reorder(uvs, fallbackOrder);
        }

        return new BakedQuad(wound[0], wound[1], wound[2], wound[3], woundUvs[0], woundUvs[1], woundUvs[2], woundUvs[3], direction, material);
    }

    private static Vector3fc[] reorder(Vector3fc[] values, int[] order) {
        if (order == null)
            return values.clone();

        return new Vector3fc[] {values[order[0]], values[order[1]], values[order[2]], values[order[3]]};
    }

    private static long[] reorder(long[] values, int[] order) {
        if (order == null)
            return values.clone();

        return new long[] {values[order[0]], values[order[1]], values[order[2]], values[order[3]]};
    }

    private static boolean isHiddenFace(Vector3fc[] positions, BlockState state, String name) {
        if (state == null)
            return false;

        if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof EnderChestBlock)
            return (name.equals("lid") || name.equals("bottom")) && isChestSeam(positions);

        if (state.getBlock() instanceof ShulkerBoxBlock)
            return (name.equals("lid") || name.equals("base")) && !touchesCorner(positions);

        return false;
    }

    private static boolean isChestSeam(Vector3fc[] positions) {
        float y = positions[0].y();

        if (positions[1].y() != y || positions[2].y() != y || positions[3].y() != y)
            return false;

        return y >= CHEST_SEAM_LOW && y <= CHEST_SEAM_HIGH;
    }

    private static boolean touchesCorner(Vector3fc[] positions) {
        for (Vector3fc position : positions)
            if (isEdge(position.x()) && isEdge(position.y()) && isEdge(position.z()))
                return true;

        return false;
    }

    private static boolean isEdge(float coordinate) {
        return coordinate <= SHULKER_CORNER || coordinate >= 1.0F - SHULKER_CORNER;
    }
}
