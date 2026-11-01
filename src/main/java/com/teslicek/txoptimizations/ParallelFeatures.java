package com.teslicek.txoptimizations;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teslicek.txoptimizations.mixin.FeaturePreparedGroupAccessor;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
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

    private static final AtomicReference<Thread>                     OWNER      = new AtomicReference<>();
    private static final Matrix4f                                    MODEL_VIEW = new Matrix4f();
    private static final FeatureWorker                               TEXT       = new FeatureWorker("TxOptimizations Text Worker");
    private static final FeatureWorker                               GEOMETRY   = new FeatureWorker("TxOptimizations Geometry Worker");
    private static final Map<FeatureRendererType<?>, FeatureWorker> ASSIGNED   = new IdentityHashMap<>();

    private static ProjectionType   projectionType;
    private static boolean          allowed;
    private static int              lockDepth;
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

        boolean text     = TEXT.claimFrame();
        boolean geometry = GEOMETRY.claimFrame();

        if (!text && !geometry)
            return;

        for (Map.Entry<FeatureRendererType<?>, List<?>> entry : groupsByFeature.entrySet()) {
            FeatureWorker worker = workerFor(entry.getKey());

            if (worker == null || !(worker == TEXT ? text : geometry) || !runsOffThread(entry.getKey(), entry.getValue(), context.font(), submits))
                continue;

            worker.batches().add(entry.getValue());
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
            TEXT.prepareUnstarted();
            GEOMETRY.prepareUnstarted();
        } finally {
            try {
                TEXT.finish(main);
            } finally {
                try {
                    GEOMETRY.finish(main);
                } finally {
                    ASSIGNED.clear();
                    active = false;
                }
            }
        }
    }

    public static boolean isWorkerThread() {
        return TEXT.isCurrentThread() || GEOMETRY.isCurrentThread();
    }

    public static boolean isPreparingOnWorker() {
        return active && OWNER.get() == Thread.currentThread() && isWorkerThread();
    }

    public static Matrix4f modelViewMatrixCopy() {
        return isWorkerThread() ? new Matrix4f(MODEL_VIEW) : RenderSystem.getModelViewMatrixCopy();
    }

    public static ProjectionType projectionType() {
        return isWorkerThread() ? projectionType : RenderSystem.getProjectionType();
    }

    public static PreparedRenderType prepare(RenderType renderType) {
        if (!lock())
            return renderType.prepare();

        try {
            return renderType.prepare();
        } finally {
            unlock();
        }
    }

    public static boolean lock() {
        if (!active || !TEXT.isRunning() && !GEOMETRY.isRunning())
            return false;

        Thread current = Thread.currentThread();

        if (OWNER.get() == current) {
            lockDepth ++;
            return true;
        }

        while (OWNER.get() != null || !OWNER.compareAndSet(null, current))
            Thread.onSpinWait();

        lockDepth = 1;

        return true;
    }

    public static void unlock() {
        if (OWNER.get() != Thread.currentThread())
            throw new IllegalStateException("The feature lock is not held by " + Thread.currentThread().getName());

        lockDepth --;

        if (lockDepth == 0)
            OWNER.set(null);
    }

    private static FeatureWorker workerFor(FeatureRendererType<?> type) {
        if (type == TextFeatureRenderer.TYPE || type == GizmoFeatureRenderer.TYPE || type == QuadParticleFeatureRenderer.TYPE)
            return TEXT;

        if (type == BlockModelFeatureRenderer.TYPE || type == ShadowFeatureRenderer.TYPE || type == FlameFeatureRenderer.TYPE || type == LeashFeatureRenderer.TYPE || type == ShapeOutlineFeatureRenderer.TYPE)
            return GEOMETRY;

        return null;
    }

    private static boolean runsOffThread(FeatureRendererType<?> type, List<?> groups, Font font, List<SubmitNode> submits) {
        if (type == TextFeatureRenderer.TYPE)
            return layoutsCached(font, groups, submits);

        if (type == GizmoFeatureRenderer.TYPE)
            return withoutTexts(groups, submits);

        return true;
    }

    private static boolean layoutsCached(Font font, List<?> groups, List<SubmitNode> submits) {
        int clears = TextLayoutCache.entryClears();

        for (Object group : groups) {
            FeaturePreparedGroupAccessor accessor = (FeaturePreparedGroupAccessor) group;

            for (int index = accessor.txoptimizations$fromInclusive(); index <= accessor.txoptimizations$toInclusive(); index ++) {
                if (!(((TextFeatureRenderer.Submit) submits.get(index)).content() instanceof TextFeatureRenderer.Content.Text text))
                    continue;

                if (text.outlineColor() == 0 ? !TextLayoutCache.ensure(font, text.string(), text.x(), text.y(), text.color(), text.dropShadow(), text.backgroundColor(), false) : !TextLayoutCache.ensure(font, text.string(), text.x(), text.y(), text.outlineColor(), false, 0, true) || !TextLayoutCache.ensure(font, text.string(), text.x(), text.y(), text.color(), false, 0, false))
                    return false;
            }
        }

        return TextLayoutCache.entryClears() == clears;
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
