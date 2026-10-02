package com.teslicek.txoptimizations.cull;

import com.mojang.logging.LogUtils;
import com.teslicek.txoptimizations.ClientClock;
import com.teslicek.txoptimizations.mixin.FrustumAccessor;
import com.teslicek.txoptimizations.mixin.cull.EntityRendererCullAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.slf4j.Logger;

public final class EntityCulling {

    private static final int    TRACING_DISTANCE      = 128;
    private static final int    BLOCK_ENTITY_DISTANCE = 64;
    private static final int    HITBOX_LIMIT          = 50;
    private static final int    CAPTURE_INTERVAL      = 5;
    private static final int    WARMUP_TICKS          = 10;
    private static final long   VISIBLE_HOLD_MILLIS   = 1000L;
    private static final double NAME_TAG_DISTANCE     = 64.0;
    private static final double BELOW_NAME_DISTANCE   = 10.0;

    private static final Logger          LOGGER  = LogUtils.getLogger();
    private static final OcclusionTracer TRACER  = new OcclusionTracer(TRACING_DISTANCE);
    private static final AtomicBoolean   PENDING = new AtomicBoolean();
    private static final Thread          WORKER  = Thread.ofPlatform().name("TxOptimizations Culling").daemon().unstarted(EntityCulling::work);

    private static volatile Snapshot snapshot;
    private static volatile Vec3     camera;

    private static int     captureTick;
    private static long    tickFrame = -1L;
    private static boolean renderedSinceTick;

    private EntityCulling() {
    }

    public static void start() {
        WORKER.start();
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level  = minecraft.level;
        LocalPlayer player = minecraft.player;
        long        frame  = ClientClock.frame();

        renderedSinceTick = frame != tickFrame;
        tickFrame         = frame;

        if (level == null || player == null || player.tickCount <= WARMUP_TICKS) {
            captureTick = 0;

            if (snapshot != null) {
                snapshot = null;
                signal();
            }

            return;
        }

        Vec3 position = minecraft.gameRenderer.mainCamera().position();

        if (captureTick == 0)
            snapshot = capture(minecraft, level, player, position);

        captureTick = (captureTick + 1) % CAPTURE_INTERVAL;
        camera      = position;

        signal();
    }

    public static boolean hidesEntity(Entity entity) {
        return ((Cullable) entity).txoptimizations$isCulled() && !entity.shouldShowName();
    }

    public static boolean hidesBlockEntity(BlockEntity blockEntity) {
        if (((Cullable) blockEntity).txoptimizations$isCulled())
            return true;

        BlockPos pos        = blockEntity.getBlockPos();
        int      extension  = verticalExtension(blockEntity);
        int      visibility = ((FrustumAccessor) Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum()).txoptimizations$cubeInFrustum(pos.getX(), pos.getY() - extension, pos.getZ(), pos.getX() + 1, pos.getY() + 1 + extension, pos.getZ() + 1);

        return visibility != FrustumIntersection.INSIDE && visibility != FrustumIntersection.INTERSECT;
    }

    public static boolean skipsTick(Entity entity) {
        Cullable cullable = (Cullable) entity;

        if (!cullable.txoptimizations$isCulled() && !(renderedSinceTick && cullable.txoptimizations$isOutOfCamera())) {
            cullable.txoptimizations$setOutOfCamera(true);

            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        return entity != minecraft.player
            && entity != minecraft.getCameraEntity()
            && !entity.isVehicle()
            && !(entity instanceof VehicleEntity)
            && !(entity instanceof Display)
            && !(entity instanceof FireworkRocketEntity)
            && ((EntityRendererCullAccessor) minecraft.getEntityRenderDispatcher().getRenderer(entity)).txoptimizations$affectedByCulling(entity);
    }

    public static EntityRenderState nameTagState(Entity entity, float partialTick) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState      state      = new EntityRenderState();
        double                 distance   = dispatcher.distanceToSqr(entity);

        state.entityType  = EntityTypes.INTERACTION;
        state.x           = Mth.lerp(partialTick, entity.xOld, entity.getX());
        state.y           = Mth.lerp(partialTick, entity.yOld, entity.getY());
        state.z           = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        state.isInvisible = true;

        if (entity.isDiscrete() || !((EntityRendererCullAccessor) dispatcher.getRenderer(entity)).txoptimizations$shouldShowName(entity, distance))
            return state;

        double nameTagDistance   = entity instanceof LivingEntity living ? living.getAttributeValue(Attributes.NAME_TAG_DISTANCE) : NAME_TAG_DISTANCE;
        double belowNameDistance = entity instanceof LivingEntity living ? living.getAttributeValue(Attributes.BELOW_NAME_DISTANCE) : BELOW_NAME_DISTANCE;

        if (distance < Mth.square(belowNameDistance))
            state.scoreText = entity.belowNameDisplay();

        if (distance < Mth.square(nameTagDistance)) {
            state.nameTag           = entity.getDisplayName();
            state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
        }

        return state;
    }

    private static Snapshot capture(Minecraft minecraft, ClientLevel level, LocalPlayer player, Vec3 position) {
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        List<Cullable>         pinned     = new ArrayList<>();
        List<EntityTarget>     entities   = new ArrayList<>();

        for (Entity entity : level.entitiesForRendering()) {
            EntityRendererCullAccessor renderer = (EntityRendererCullAccessor) dispatcher.getRenderer(entity);

            if (entity == player || minecraft.shouldEntityAppearGlowing(entity) || !renderer.txoptimizations$affectedByCulling(entity)) {
                pinned.add((Cullable) entity);
                continue;
            }

            entities.add(new EntityTarget((Cullable) entity, entity.getX(), entity.getY(), entity.getZ(), cullingBox(entity, renderer)));
        }

        return new Snapshot(level, pinned, entities, captureBlockEntities(minecraft.getBlockEntityRenderDispatcher(), level.getChunkSource(), position));
    }

    private static AABB cullingBox(Entity entity, EntityRendererCullAccessor renderer) {
        if (entity instanceof ArmorStand armorStand && armorStand.isMarker())
            return EntityTypes.ARMOR_STAND.getDimensions().makeBoundingBox(entity.getX(), entity.getY(), entity.getZ());

        return renderer.txoptimizations$getBoundingBoxForCulling(entity, 0.0F);
    }

    private static List<BlockEntity> captureBlockEntities(BlockEntityRenderDispatcher dispatcher, ClientChunkCache chunks, Vec3 position) {
        List<BlockEntity> blockEntities = new ArrayList<>();
        int               minChunkX     = SectionPos.blockToSectionCoord(position.x - BLOCK_ENTITY_DISTANCE);
        int               minChunkZ     = SectionPos.blockToSectionCoord(position.z - BLOCK_ENTITY_DISTANCE);
        int               maxChunkX     = SectionPos.blockToSectionCoord(position.x + BLOCK_ENTITY_DISTANCE);
        int               maxChunkZ     = SectionPos.blockToSectionCoord(position.z + BLOCK_ENTITY_DISTANCE);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX ++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ ++) {
                LevelChunk chunk = chunks.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);

                if (chunk == null)
                    continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockEntityRenderer<?, ?> renderer = dispatcher.getRenderer(blockEntity);

                    if (renderer != null && !renderer.shouldRenderOffScreen())
                        blockEntities.add(blockEntity);
                }
            }
        }

        return blockEntities;
    }

    private static void signal() {
        PENDING.set(true);
        LockSupport.unpark(WORKER);
    }

    private static void work() {
        Snapshot lastSnapshot = null;
        Vec3     lastCamera   = null;

        while (true) {
            LockSupport.park();

            if (!PENDING.getAndSet(false))
                continue;

            Snapshot current  = snapshot;
            Vec3     position = camera;

            if (current == null) {
                lastSnapshot = null;
                continue;
            }

            if (current == lastSnapshot && position.equals(lastCamera))
                continue;

            lastSnapshot = current;
            lastCamera   = position;

            try {
                cull(current, position);
            } catch (RuntimeException exception) {
                LOGGER.error("Entity culling pass failed", exception);
            }
        }
    }

    private static void cull(Snapshot current, Vec3 position) {
        long now = System.currentTimeMillis();

        TRACER.begin(current.level(), position);

        try {
            for (Cullable cullable : current.pinned())
                reveal(cullable, now);

            for (BlockEntity blockEntity : current.blockEntities())
                cullBlockEntity(blockEntity, position, now);

            for (EntityTarget target : current.entities())
                cullEntity(target, position, now);
        } finally {
            TRACER.finish();
        }
    }

    private static void cullBlockEntity(BlockEntity blockEntity, Vec3 position, long now) {
        Cullable cullable = (Cullable) blockEntity;

        if (cullable.txoptimizations$getVisibleUntil() > now)
            return;

        BlockPos pos = blockEntity.getBlockPos();

        if (position.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= BLOCK_ENTITY_DISTANCE * BLOCK_ENTITY_DISTANCE) {
            reveal(cullable, now);

            return;
        }

        int extension = verticalExtension(blockEntity);

        apply(cullable, TRACER.isVisible(pos.getX(), pos.getY() - extension, pos.getZ(), pos.getX() + 1, pos.getY() + 1 + extension, pos.getZ() + 1), now);
    }

    private static void cullEntity(EntityTarget target, Vec3 position, long now) {
        Cullable cullable = target.cullable();

        if (cullable.txoptimizations$getVisibleUntil() > now)
            return;

        AABB box = target.box();

        if (position.distanceToSqr(target.x(), target.y(), target.z()) >= TRACING_DISTANCE * TRACING_DISTANCE || box.getXsize() > HITBOX_LIMIT || box.getYsize() > HITBOX_LIMIT || box.getZsize() > HITBOX_LIMIT) {
            reveal(cullable, now);

            return;
        }

        apply(cullable, TRACER.isVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ), now);
    }

    private static void apply(Cullable cullable, boolean visible, long now) {
        if (visible) {
            reveal(cullable, now);

            return;
        }

        cullable.txoptimizations$setCulled(true);
    }

    private static void reveal(Cullable cullable, long now) {
        cullable.txoptimizations$setCulled(false);
        cullable.txoptimizations$setVisibleUntil(now + VISIBLE_HOLD_MILLIS);
    }

    private static int verticalExtension(BlockEntity blockEntity) {
        return blockEntity instanceof BannerBlockEntity ? 1 : 0;
    }

    private record Snapshot(ClientLevel level, List<Cullable> pinned, List<EntityTarget> entities, List<BlockEntity> blockEntities) {
    }

    private record EntityTarget(Cullable cullable, double x, double y, double z, AABB box) {
    }
}
