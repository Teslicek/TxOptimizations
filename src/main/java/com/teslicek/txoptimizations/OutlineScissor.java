package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.mixin.FeaturePreparedGroupAccessor;
import com.teslicek.txoptimizations.mixin.PostChainPassesAccessor;
import com.teslicek.txoptimizations.mixin.RenderTypeFeatureRendererGroupsAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4d;
import org.joml.Matrix4fc;
import org.joml.Vector4d;

public final class OutlineScissor {

    private static final int    RASTER_REACH = 2;
    private static final int    SOBEL_REACH  = 2;
    private static final int    BLUR_REACH   = 4;
    private static final int    MARGIN       = RASTER_REACH + SOBEL_REACH + BLUR_REACH;
    private static final int    PASSES       = 4;
    private static final double MIN_CLIP_W   = 0.01;
    private static final int    UNMEASURED   = 0;
    private static final int    UNBOUNDED    = 1;
    private static final int    BOUNDED      = 2;
    private static final int    BOX_VALUES   = 4;
    private static final int    RECT_VALUES  = 4;

    private static final String[] VANILLA_FILES = {
        "post_effect/entity_outline.json",
        "shaders/post/entity_sobel.fsh",
        "shaders/post/entity_outline_box_blur.fsh",
        "shaders/post/blit.fsh",
        "shaders/core/screenquad.vsh",
        "shaders/core/blit_screen.fsh",
        "shaders/core/rendertype_outline.vsh",
        "shaders/include/dynamictransforms.glsl",
        "shaders/include/projection.glsl"
    };

    private static final ClassValue<Boolean> PLAIN_EXECUTION = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("executeGroup", FeatureFrameContext.class, OitStage.class, RenderPass.class, int.class, List.class, boolean.class).getDeclaringClass() == RenderTypeFeatureRenderer.class;
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Feature renderer " + type.getName() + " has no executeGroup method", exception);
            }
        }
    };

    private static final Matrix4d MODEL_VIEW = new Matrix4d();
    private static final Matrix4d CLIP       = new Matrix4d();
    private static final Vector4d CORNER     = new Vector4d();

    private static PostChain       chain;
    private static List<PostPass>  passes = List.of();
    private static boolean         vanilla;
    private static int             state  = UNMEASURED;
    private static boolean         blitting;
    private static double[]        boxes  = new double[BOX_VALUES * 8];
    private static int             boxCount;
    private static int[]           rects  = new int[0];
    private static int             rectsWidth;
    private static int             rectsHeight;

    private OutlineScissor() {
    }

    public static void useChain(PostChain current) {
        if (current == chain)
            return;

        chain = current;

        if (current == null) {
            passes  = List.of();
            vanilla = false;
            return;
        }

        passes  = ((PostChainPassesAccessor) current).txoptimizations$passes();
        vanilla = passes.size() == PASSES && isVanillaChain();
    }

    public static void beginFrame() {
        state = UNMEASURED;
        rects = null;
    }

    public static void measure(SubmitNodeStorage storage, Map<FeatureRenderPhase<?>, ? extends List<?>> groupsByPhase, FeatureRendererMap renderers, StagedVertexBuffer vertices) {
        state = UNBOUNDED;

        if (!vanilla || !LevelProjection.isBound())
            return;

        ((BackgroundVertexCopy) vertices).txoptimizations$finishOutlineBounds();

        boxCount = 0;
        rects    = null;

        for (SubmitNodeCollection collection : storage.getSubmitsPerOrder().values()) {
            List<?> groups = groupsByPhase.get(collection.outline);

            if (groups == null)
                continue;

            for (Object group : groups) {
                if (!includeGroup((FeaturePreparedGroupAccessor) group, renderers))
                    return;
            }
        }

        state = BOUNDED;
    }

    public static boolean isOutlineChain(PostChain current) {
        return vanilla && current == chain;
    }

    public static boolean isOutlinePass(PostPass pass) {
        return vanilla && passes.contains(pass);
    }

    public static boolean isBlitting() {
        return blitting;
    }

    public static void beginBlit() {
        blitting = vanilla;
    }

    public static void endBlit() {
        blitting = false;
        state    = UNMEASURED;
    }

    public static void draw(RenderPass renderPass, int width, int height, int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        requireMeasured();

        if (state == UNBOUNDED) {
            renderPass.draw(vertexCount, instanceCount, firstVertex, firstInstance);
            return;
        }

        int[] areas = rects(width, height);

        for (int at = 0; at < areas.length; at += RECT_VALUES) {
            renderPass.enableScissor(areas[at], areas[at + 1], areas[at + 2] - areas[at], areas[at + 3] - areas[at + 1]);
            renderPass.draw(vertexCount, instanceCount, firstVertex, firstInstance);
        }
    }

    private static boolean includeGroup(FeaturePreparedGroupAccessor group, FeatureRendererMap renderers) {
        FeatureRenderer<?> renderer = renderers.getOrThrow(group.txoptimizations$featureType());

        if (!(renderer instanceof RenderTypeFeatureRenderer<?> typed) || !PLAIN_EXECUTION.get(renderer.getClass()))
            return false;

        OutlineDrawGroup              drawGroup = (OutlineDrawGroup) ((RenderTypeFeatureRendererGroupsAccessor) typed).txoptimizations$groups().get(group.txoptimizations$featureGroupIndex());
        List<StagedVertexBuffer.Draw> draws     = drawGroup.txoptimizations$draws();
        List<PreparedRenderType>      types     = drawGroup.txoptimizations$drawRenderTypes();

        for (int index = 0; index < draws.size(); index ++) {
            if (!includeDraw(draws.get(index), types.get(index)))
                return false;
        }

        return true;
    }

    private static boolean includeDraw(StagedVertexBuffer.Draw draw, PreparedRenderType type) {
        if (draw.isEmpty())
            return true;

        RenderPipeline pipeline = type.pipeline();

        if (pipeline != RenderPipelines.OUTLINE_CULL && pipeline != RenderPipelines.OUTLINE_NO_CULL)
            return false;

        float[]   boxes     = ((OutlineDraw) draw).txoptimizations$outlineBoxes();
        Matrix4fc modelView = ((OutlineModelView) (Object) type).txoptimizations$modelView();

        if (boxes == null || modelView == null)
            return false;

        CLIP.set(LevelProjection.matrix()).mul(MODEL_VIEW.set(modelView));

        for (int offset = 0; offset < boxes.length; offset += PositionBounds.VALUES) {
            if (!includeBox(boxes, offset))
                return false;
        }

        return true;
    }

    private static boolean includeBox(float[] bounds, int offset) {
        for (int value = offset; value < offset + PositionBounds.VALUES; value ++) {
            if (!Float.isFinite(bounds[value]))
                return false;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (int corner = 0; corner < 8; corner ++) {
            float x = bounds[offset + ((corner & 1) == 0 ? 0 : 3)];
            float y = bounds[offset + ((corner & 2) == 0 ? 1 : 4)];
            float z = bounds[offset + ((corner & 4) == 0 ? 2 : 5)];

            CLIP.transform(CORNER.set(x, y, z, 1.0));

            if (!(CORNER.w >= MIN_CLIP_W))
                return false;

            double ndcX = CORNER.x / CORNER.w;
            double ndcY = CORNER.y / CORNER.w;

            if (!Double.isFinite(ndcX) || !Double.isFinite(ndcY))
                return false;

            minX = Math.min(minX, ndcX);
            minY = Math.min(minY, ndcY);
            maxX = Math.max(maxX, ndcX);
            maxY = Math.max(maxY, ndcY);
        }

        if (boxCount * BOX_VALUES == boxes.length)
            boxes = Arrays.copyOf(boxes, boxes.length * 2);

        int at = boxCount * BOX_VALUES;

        boxes[at]     = minX;
        boxes[at + 1] = minY;
        boxes[at + 2] = maxX;
        boxes[at + 3] = maxY;
        boxCount ++;

        return true;
    }

    private static int[] rects(int width, int height) {
        if (rects != null && rectsWidth == width && rectsHeight == height)
            return rects;

        int[] areas = new int[boxCount * RECT_VALUES];
        int   count = 0;

        for (int box = 0; box < boxCount; box ++) {
            int at     = box * BOX_VALUES;
            int startX = (int) Math.max(0.0, Math.floor((boxes[at] + 1.0) * 0.5 * width) - MARGIN);
            int startY = (int) Math.max(0.0, Math.floor((boxes[at + 1] + 1.0) * 0.5 * height) - MARGIN);
            int endX   = (int) Math.min(width, Math.ceil((boxes[at + 2] + 1.0) * 0.5 * width) + MARGIN);
            int endY   = (int) Math.min(height, Math.ceil((boxes[at + 3] + 1.0) * 0.5 * height) + MARGIN);

            if (startX >= endX || startY >= endY)
                continue;

            int rect = count * RECT_VALUES;

            areas[rect]     = startX;
            areas[rect + 1] = startY;
            areas[rect + 2] = endX;
            areas[rect + 3] = endY;
            count ++;
        }

        count = mergeOverlaps(areas, count);

        rects       = Arrays.copyOf(areas, count * RECT_VALUES);
        rectsWidth  = width;
        rectsHeight = height;

        return rects;
    }

    private static int mergeOverlaps(int[] areas, int count) {
        boolean merged = true;

        while (merged) {
            merged = false;

            for (int first = 0; first < count && !merged; first ++) {
                for (int second = first + 1; second < count; second ++) {
                    int a = first * RECT_VALUES;
                    int b = second * RECT_VALUES;

                    if (areas[a] >= areas[b + 2] || areas[b] >= areas[a + 2] || areas[a + 1] >= areas[b + 3] || areas[b + 1] >= areas[a + 3])
                        continue;

                    areas[a]     = Math.min(areas[a], areas[b]);
                    areas[a + 1] = Math.min(areas[a + 1], areas[b + 1]);
                    areas[a + 2] = Math.max(areas[a + 2], areas[b + 2]);
                    areas[a + 3] = Math.max(areas[a + 3], areas[b + 3]);

                    int last = (count - 1) * RECT_VALUES;

                    System.arraycopy(areas, last, areas, b, RECT_VALUES);
                    count --;
                    merged = true;
                    break;
                }
            }
        }

        return count;
    }

    private static void requireMeasured() {
        if (state == UNMEASURED)
            throw new IllegalStateException("The entity outline chain ran before its geometry bounds were measured this frame");
    }

    private static boolean isVanillaChain() {
        for (String file : VANILLA_FILES) {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace(file));

            if (resource.isEmpty())
                throw new IllegalStateException("Vanilla resource " + file + " is missing");

            if (!resource.get().sourcePackId().equals(BuiltInPackSource.VANILLA_ID))
                return false;
        }

        return true;
    }
}
