package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.teslicek.txoptimizations.mixin.FeaturePreparedGroupAccessor;
import com.teslicek.txoptimizations.mixin.PostChainPassesAccessor;
import com.teslicek.txoptimizations.mixin.RenderTypeFeatureRendererGroupsAccessor;
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
    private static double          minX;
    private static double          minY;
    private static double          maxX;
    private static double          maxY;

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
    }

    public static void measure(SubmitNodeStorage storage, Map<FeatureRenderPhase<?>, ? extends List<?>> groupsByPhase, FeatureRendererMap renderers, StagedVertexBuffer vertices) {
        state = UNBOUNDED;

        if (!vanilla || !LevelProjection.isBound())
            return;

        ((BackgroundVertexCopy) vertices).txoptimizations$finishOutlineBounds();

        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

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

    public static boolean isEmpty(int width, int height) {
        requireMeasured();

        if (state == UNBOUNDED)
            return false;

        return startX(width) >= endX(width) || startY(height) >= endY(height);
    }

    public static void enableScissor(RenderPass renderPass, int width, int height) {
        requireMeasured();

        if (state == UNBOUNDED)
            return;

        int startX = startX(width);
        int startY = startY(height);

        renderPass.enableScissor(startX, startY, endX(width) - startX, endY(height) - startY);
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

    private static boolean includeBox(float[] boxes, int offset) {
        for (int value = offset; value < offset + PositionBounds.VALUES; value ++) {
            if (!Float.isFinite(boxes[value]))
                return false;
        }

        for (int corner = 0; corner < 8; corner ++) {
            float x = boxes[offset + ((corner & 1) == 0 ? 0 : 3)];
            float y = boxes[offset + ((corner & 2) == 0 ? 1 : 4)];
            float z = boxes[offset + ((corner & 4) == 0 ? 2 : 5)];

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

        return true;
    }

    private static int startX(int width) {
        return (int) Math.max(0.0, Math.floor((minX + 1.0) * 0.5 * width) - MARGIN);
    }

    private static int endX(int width) {
        return (int) Math.min(width, Math.ceil((maxX + 1.0) * 0.5 * width) + MARGIN);
    }

    private static int startY(int height) {
        return (int) Math.max(0.0, Math.floor((minY + 1.0) * 0.5 * height) - MARGIN);
    }

    private static int endY(int height) {
        return (int) Math.min(height, Math.ceil((maxY + 1.0) * 0.5 * height) + MARGIN);
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
