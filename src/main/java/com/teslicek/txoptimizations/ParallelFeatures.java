package com.teslicek.txoptimizations;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teslicek.txoptimizations.mixin.FeaturePreparedGroupAccessor;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;

public final class ParallelFeatures {

    private static final ReentrantLock                               LOCK       = new ReentrantLock();
    private static final Matrix4f                                    MODEL_VIEW = new Matrix4f();
    private static final FeatureWorker                               TEXT       = new FeatureWorker("TxOptimizations Text Worker");
    private static final FeatureWorker                               GEOMETRY   = new FeatureWorker("TxOptimizations Geometry Worker");
    private static final Map<FeatureRendererType<?>, FeatureWorker> ASSIGNED   = new IdentityHashMap<>();

    private static ProjectionType   projectionType;
    private static boolean          allowed;
    private static volatile boolean active;

    private ParallelFeatures() {
    }

    public static void allowNextFrame(boolean allow) {
        allowed = allow;
    }

    public static void start(FeatureFrameContext context, Map<FeatureRendererType<?>, List<?>> groupsByFeature, FeatureRendererMap renderers, List<SubmitNode> submits) {
        if (!allowed)
            return;

        allowed = false;

        for (Map.Entry<FeatureRendererType<?>, List<?>> entry : groupsByFeature.entrySet()) {
            FeatureWorker worker = workerFor(entry.getKey(), entry.getValue(), context.font(), submits);

            if (worker == null)
                continue;

            worker.groups().addAll(entry.getValue());
            ASSIGNED.put(entry.getKey(), worker);
        }

        if (ASSIGNED.isEmpty())
            return;

        MODEL_VIEW.set(RenderSystem.getModelViewMatrixCopy());

        projectionType = RenderSystem.getProjectionType();
        active         = true;

        TEXT.start(context, renderers, submits);
        GEOMETRY.start(context, renderers, submits);
    }

    public static boolean runsOnWorker(Object group) {
        return active && ASSIGNED.containsKey(((FeaturePreparedGroupAccessor) group).txoptimizations$featureType());
    }

    public static void finish(StagedVertexBuffer main) {
        if (!active)
            return;

        try {
            TEXT.finish(main);
            GEOMETRY.finish(main);
        } finally {
            ASSIGNED.clear();
            active = false;
        }
    }

    public static boolean isWorkerThread() {
        return TEXT.isCurrentThread() || GEOMETRY.isCurrentThread();
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

    private static FeatureWorker workerFor(FeatureRendererType<?> type, List<?> groups, Font font, List<SubmitNode> submits) {
        if (type == TextFeatureRenderer.TYPE)
            return layoutsCached(font, groups, submits) ? TEXT : null;

        if (type == GizmoFeatureRenderer.TYPE)
            return withoutTexts(groups, submits) ? TEXT : null;

        if (type == QuadParticleFeatureRenderer.TYPE)
            return TEXT;

        if (type == ItemFeatureRenderer.TYPE || type == BlockModelFeatureRenderer.TYPE || type == ShadowFeatureRenderer.TYPE || type == FlameFeatureRenderer.TYPE || type == LeashFeatureRenderer.TYPE || type == ShapeOutlineFeatureRenderer.TYPE)
            return GEOMETRY;

        return null;
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
}
