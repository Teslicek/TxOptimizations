package com.teslicek.txoptimizations;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teslicek.txoptimizations.mixin.FeaturePreparedGroupAccessor;
import com.teslicek.txoptimizations.mixin.StagedVertexBufferListAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;

public final class ParallelFeatures {

    private static final int INITIAL_CAPACITY = 1 << 20;

    private static final AtomicReference<Runnable> JOB        = new AtomicReference<>();
    private static final ReentrantLock             LOCK       = new ReentrantLock();
    private static final Matrix4f                  MODEL_VIEW = new Matrix4f();
    private static final List<Object>              GROUPS     = new ArrayList<>();
    private static final Thread                    WORKER     = Thread.ofPlatform().name("TxOptimizations Feature Worker").daemon().priority(Thread.MAX_PRIORITY).start(ParallelFeatures::work);

    private static StagedVertexBuffer shard;
    private static ProjectionType     projectionType;
    private static boolean            allowed;
    private static boolean            texts;
    private static boolean            gizmos;
    private static volatile boolean   active;
    private static volatile boolean   done;
    private static volatile Throwable failure;

    private ParallelFeatures() {
    }

    public static void allowNextFrame(boolean allow) {
        allowed = allow;
    }

    public static void start(FeatureFrameContext context, Map<FeatureRendererType<?>, List<?>> groupsByFeature, FeatureRendererMap renderers, List<SubmitNode> submits) {
        if (!allowed)
            return;

        allowed = false;

        List<?> textGroups  = groupsByFeature.get(TextFeatureRenderer.TYPE);
        List<?> gizmoGroups = groupsByFeature.get(GizmoFeatureRenderer.TYPE);

        texts  = textGroups != null && layoutsCached(context.font(), textGroups, submits);
        gizmos = gizmoGroups != null && withoutTexts(gizmoGroups, submits);

        if (!texts && !gizmos)
            return;

        GROUPS.clear();

        for (Map.Entry<FeatureRendererType<?>, List<?>> entry : groupsByFeature.entrySet()) {
            if (isWorkerType(entry.getKey()))
                GROUPS.addAll(entry.getValue());
        }

        if (shard == null)
            shard = new StagedVertexBuffer((Supplier<String>) () -> "TxOptimizations feature worker", INITIAL_CAPACITY);

        FeatureFrameContext worker = new FeatureFrameContext(context.options(), context.font(), context.blockStateModelSet(), context.blockColors(), context.textureManager(), context.atlasManager(), context.lightmap(), shard);

        MODEL_VIEW.set(RenderSystem.getModelViewMatrixCopy());

        projectionType = RenderSystem.getProjectionType();
        done           = false;
        failure        = null;
        active         = true;

        JOB.set(() -> prepare(worker, renderers, submits));
        LockSupport.unpark(WORKER);
    }

    public static boolean runsOnWorker(Object group) {
        return active && isWorkerType(((FeaturePreparedGroupAccessor) group).txoptimizations$featureType());
    }

    public static void finish(StagedVertexBuffer main) {
        if (!active)
            return;

        while (!done)
            Thread.onSpinWait();

        active = false;

        Throwable thrown = failure;

        if (thrown != null) {
            failure = null;

            throw new IllegalStateException("Preparing features on the feature worker failed", thrown);
        }

        List<StagedVertexBuffer.Draw> shardDraws = ((StagedVertexBufferListAccessor) shard).txoptimizations$draws();
        List<StagedVertexBuffer.Draw> mainDraws  = ((StagedVertexBufferListAccessor) main).txoptimizations$draws();

        for (StagedVertexBuffer.Draw draw : shardDraws) {
            mainDraws.add(draw);
            main.requestIndexCount(draw);
        }

        shardDraws.clear();
    }

    public static boolean isWorkerThread() {
        return Thread.currentThread() == WORKER;
    }

    public static boolean isPreparingOnWorker() {
        return active && LOCK.isHeldByCurrentThread() && isWorkerThread();
    }

    public static Matrix4f modelViewMatrixCopy() {
        return isWorkerThread() ? new Matrix4f(MODEL_VIEW) : RenderSystem.getModelViewMatrixCopy();
    }

    public static ProjectionType projectionType() {
        return isWorkerThread() ? projectionType : RenderSystem.getProjectionType();
    }

    public static PreparedRenderType prepare(RenderType renderType) {
        if (!active)
            return renderType.prepare();

        LOCK.lock();

        try {
            return renderType.prepare();
        } finally {
            LOCK.unlock();
        }
    }

    public static void lock() {
        if (active)
            LOCK.lock();
    }

    public static void unlock() {
        if (LOCK.isHeldByCurrentThread())
            LOCK.unlock();
    }

    private static boolean isWorkerType(FeatureRendererType<?> type) {
        return texts && type == TextFeatureRenderer.TYPE || gizmos && type == GizmoFeatureRenderer.TYPE;
    }

    private static boolean layoutsCached(Font font, List<?> groups, List<SubmitNode> submits) {
        for (Object group : groups) {
            FeaturePreparedGroupAccessor accessor = (FeaturePreparedGroupAccessor) group;

            for (int index = accessor.txoptimizations$fromInclusive(); index <= accessor.txoptimizations$toInclusive(); index ++) {
                if (!(((TextFeatureRenderer.Submit) submits.get(index)).content() instanceof TextFeatureRenderer.Content.Text text))
                    continue;

                if (text.outlineColor() == 0 ? !TextLayoutCache.ensure(font, text.string(), text.x(), text.y(), text.color(), text.dropShadow(), text.backgroundColor(), false) : !TextLayoutCache.ensure(font, text.string(), text.x(), text.y(), text.outlineColor(), false, 0, true) || !TextLayoutCache.ensure(font, text.string(), text.x(), text.y(), text.color(), false, 0, false))
                    return false;
            }
        }

        return true;
    }

    private static boolean withoutTexts(List<?> groups, List<SubmitNode> submits) {
        for (Object group : groups) {
            FeaturePreparedGroupAccessor accessor = (FeaturePreparedGroupAccessor) group;

            for (int index = accessor.txoptimizations$fromInclusive(); index <= accessor.txoptimizations$toInclusive(); index ++) {
                if (!((GizmoFeatureRenderer.Submit) submits.get(index)).group().texts().isEmpty())
                    return false;
            }
        }

        return true;
    }

    private static void prepare(FeatureFrameContext context, FeatureRendererMap renderers, List<SubmitNode> submits) {
        for (Object group : GROUPS)
            ((FeaturePreparedGroupAccessor) group).txoptimizations$prepare(context, renderers, submits);

        ((StagedVertexBufferListAccessor) shard).txoptimizations$finishLastVertexBuilder();
    }

    private static void work() {
        while (true) {
            Runnable job = JOB.getAndSet(null);

            if (job == null) {
                LockSupport.park();
                continue;
            }

            try {
                job.run();
            } catch (Throwable thrown) {
                failure = thrown;
            } finally {
                done = true;
            }
        }
    }
}
