package com.teslicek.txoptimizations.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.CopperGolemStatueBlockRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState.ChestMaterialType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public enum BakedKind {

    CHEST(false) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            return ChestRenderer.LAYERS.select(state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE));
        }

        @Override
        public Identifier texture(BlockState state) {
            return Sheets.chooseSprite(chestMaterial(state.getBlock()), state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE)).texture();
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
            poseStack.mulPose(ChestRenderer.modelTransformation(state.getValue(ChestBlock.FACING)));
        }
    },

    BELL(true) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            return ModelLayers.BELL;
        }

        @Override
        public Identifier texture(BlockState state) {
            return BellRenderer.BELL_TEXTURE.texture();
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
        }
    },

    SKULL(false) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            if (!(((AbstractSkullBlock) state.getBlock()).getType() instanceof SkullBlock.Types type))
                return null;

            return switch (type) {
                case SKELETON -> ModelLayers.SKELETON_SKULL;
                case WITHER_SKELETON -> ModelLayers.WITHER_SKELETON_SKULL;
                case PLAYER -> ModelLayers.PLAYER_HEAD;
                case ZOMBIE -> ModelLayers.ZOMBIE_HEAD;
                case CREEPER -> ModelLayers.CREEPER_HEAD;
                case DRAGON -> ModelLayers.DRAGON_SKULL;
                case PIGLIN -> ModelLayers.PIGLIN_HEAD;
            };
        }

        @Override
        public Identifier texture(BlockState state) {
            return blockAtlasId(SkullBlockRenderer.SKIN_BY_TYPE.get(((AbstractSkullBlock) state.getBlock()).getType()));
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
            if (state.getBlock() instanceof WallSkullBlock)
                poseStack.mulPose(SkullBlockRenderer.TRANSFORMATIONS.wallTransformation(state.getValue(WallSkullBlock.FACING)));
            else
                poseStack.mulPose(SkullBlockRenderer.TRANSFORMATIONS.freeTransformations(state.getValue(SkullBlock.ROTATION)));
        }
    },

    BANNER(true) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            return state.getBlock() instanceof WallBannerBlock ? ModelLayers.WALL_BANNER : ModelLayers.STANDING_BANNER;
        }

        @Override
        public Identifier texture(BlockState state) {
            return Sheets.BANNER_BASE.texture();
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
            if (state.getBlock() instanceof WallBannerBlock)
                poseStack.mulPose(BannerRenderer.TRANSFORMATIONS.wallTransformation(state.getValue(WallBannerBlock.FACING)));
            else
                poseStack.mulPose(BannerRenderer.TRANSFORMATIONS.freeTransformations(state.getValue(BannerBlock.ROTATION)));
        }
    },

    SHULKER_BOX(false) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            return ModelLayers.SHULKER_BOX;
        }

        @Override
        public Identifier texture(BlockState state) {
            if (!(state.getBlock() instanceof ShulkerBoxBlock block))
                return null;

            DyeColor color = block.getColor();

            return color == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION.texture() : Sheets.getShulkerBoxSprite(color).texture();
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
            poseStack.mulPose(ShulkerBoxRenderer.modelTransform(state.getValue(ShulkerBoxBlock.FACING)));
        }
    },

    DECORATED_POT(false) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            return ModelLayers.DECORATED_POT_BASE;
        }

        @Override
        public Identifier texture(BlockState state) {
            return Sheets.DECORATED_POT_BASE.texture();
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
            poseStack.mulPose(DecoratedPotRenderer.modelTransformation(state.getValue(DecoratedPotBlock.HORIZONTAL_FACING)));
        }
    },

    COPPER_GOLEM_STATUE(true) {
        @Override
        public ModelLayerLocation layer(BlockState state) {
            return switch (state.getValue(CopperGolemStatueBlock.POSE)) {
                case STANDING -> ModelLayers.COPPER_GOLEM;
                case RUNNING -> ModelLayers.COPPER_GOLEM_RUNNING;
                case SITTING -> ModelLayers.COPPER_GOLEM_SITTING;
                case STAR -> ModelLayers.COPPER_GOLEM_STAR;
            };
        }

        @Override
        public Identifier texture(BlockState state) {
            return blockAtlasId(CopperGolemOxidationLevels.getOxidationLevel(((CopperGolemStatueBlock) state.getBlock()).getWeatheringState()).texture());
        }

        @Override
        public void transform(BlockState state, PoseStack poseStack) {
            poseStack.mulPose(CopperGolemStatueBlockRenderer.modelTransformation(state.getValue(CopperGolemStatueBlock.FACING)));
            poseStack.rotate(Axis.ZP.rotationDegrees(180.0F));
        }
    };

    private final boolean ambientOcclusion;

    BakedKind(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
    }

    public abstract ModelLayerLocation layer(BlockState state);

    public abstract Identifier texture(BlockState state);

    public abstract void transform(BlockState state, PoseStack poseStack);

    public boolean hasAmbientOcclusion() {
        return this.ambientOcclusion;
    }

    public boolean canBake(BlockState state) {
        return this.layer(state) != null && this.texture(state) != null;
    }

    public static BakedKind of(BlockState state) {
        return Lookup.BY_BLOCK.get(state.getBlock());
    }

    public static BakedKind of(BlockEntityType<?> type) {
        return Lookup.BY_TYPE.get(type);
    }

    public static Identifier blockAtlasId(Identifier texture) {
        return Identifier.fromNamespaceAndPath(texture.getNamespace(), texture.getPath().replace(".png", "").replace("textures/", ""));
    }

    private static ChestMaterialType chestMaterial(Block block) {
        if (block instanceof CopperChestBlock copperChest)
            return switch (copperChest.getState()) {
                case UNAFFECTED -> ChestMaterialType.COPPER_UNAFFECTED;
                case EXPOSED -> ChestMaterialType.COPPER_EXPOSED;
                case WEATHERED -> ChestMaterialType.COPPER_WEATHERED;
                case OXIDIZED -> ChestMaterialType.COPPER_OXIDIZED;
            };

        if (block instanceof EnderChestBlock)
            return ChestMaterialType.ENDER_CHEST;

        if (ChestRenderer.xmasTextures())
            return ChestMaterialType.CHRISTMAS;

        return block instanceof TrappedChestBlock ? ChestMaterialType.TRAPPED : ChestMaterialType.REGULAR;
    }

    private static final class Lookup {

        private static final Map<BlockEntityType<?>, BakedKind> BY_TYPE  = new IdentityHashMap<>();
        private static final Map<Block, BakedKind>              BY_BLOCK = new IdentityHashMap<>();

        static {
            register(CHEST, List.of(BlockEntityTypes.CHEST, BlockEntityTypes.ENDER_CHEST, BlockEntityTypes.TRAPPED_CHEST));
            register(BELL, List.of(BlockEntityTypes.BELL));
            register(SKULL, List.of(BlockEntityTypes.SKULL));
            register(BANNER, List.of(BlockEntityTypes.BANNER));
            register(SHULKER_BOX, List.of(BlockEntityTypes.SHULKER_BOX));
            register(DECORATED_POT, List.of(BlockEntityTypes.DECORATED_POT));
            register(COPPER_GOLEM_STATUE, List.of(BlockEntityTypes.COPPER_GOLEM_STATUE));
        }

        private static void register(BakedKind kind, List<BlockEntityType<?>> types) {
            for (BlockEntityType<?> type : types) {
                BY_TYPE.put(type, kind);

                for (Block block : type.validBlocks) {
                    BakedKind previous = BY_BLOCK.put(block, kind);

                    if (previous != null && previous != kind)
                        throw new IllegalStateException("Block " + block + " belongs to both " + previous + " and " + kind);
                }
            }
        }
    }
}
