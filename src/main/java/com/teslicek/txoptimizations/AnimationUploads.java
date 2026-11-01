package com.teslicek.txoptimizations;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;

public final class AnimationUploads {

    private static List<Upload> pending = new ArrayList<>();

    private AnimationUploads() {
    }

    public static void add(AtlasAnimation animation, AtlasAnimation.PendingDraw draw, GpuTextureView[] mipViews) {
        pending.add(new Upload(animation, draw, mipViews));
    }

    public static List<Upload> takeForFrame() {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException("Animation uploads were taken on the render worker");

        if (pending.isEmpty())
            return List.of();

        List<Upload> frame = pending;

        pending = new ArrayList<>();

        return frame;
    }

    public static void submit(List<Upload> uploads) {
        for (Upload upload : uploads)
            upload.animation.submit(upload.draw, upload.mipViews);
    }

    public static void drop(AtlasAnimation animation) {
        if (RenderWorker.isCurrent())
            throw new IllegalStateException("Animation uploads were dropped on the render worker");

        pending.removeIf(upload -> upload.animation == animation);
    }

    public record Upload(AtlasAnimation animation, AtlasAnimation.PendingDraw draw, GpuTextureView[] mipViews) {
    }
}
