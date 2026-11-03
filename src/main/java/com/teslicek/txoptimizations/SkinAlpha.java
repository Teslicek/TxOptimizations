package com.teslicek.txoptimizations;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jspecify.annotations.Nullable;

public final class SkinAlpha {

    private static final String                         CUTOUT  = "ALPHA_CUTOUT";
    private static final String                         SAMPLER = "Sampler0";
    private static final int                            MARGIN  = 1;
    private static final Map<GpuTextureView, SkinAlpha> SKINS   = new ConcurrentHashMap<>();
    private static final Map<RenderPipeline, Boolean>   CUTOUTS = new ConcurrentHashMap<>();

    private final int                          width;
    private final int                          height;
    private final long[]                       visible;
    private final Map<ModelPart.Cube, Answer>  hidden = new ConcurrentHashMap<>();

    private SkinAlpha(NativeImage pixels) {
        this.width   = pixels.getWidth();
        this.height  = pixels.getHeight();
        this.visible = new long[(this.width * this.height + Long.SIZE - 1) / Long.SIZE];

        for (int y = 0; y < this.height; y ++) {
            for (int x = 0; x < this.width; x ++) {
                if (pixels.getPixel(x, y) >>> 24 != 0)
                    this.visible[(y * this.width + x) / Long.SIZE] |= 1L << (y * this.width + x) % Long.SIZE;
            }
        }
    }

    public static void register(DynamicTexture texture) {
        NativeImage pixels = texture.getPixels();

        if (pixels == null || pixels.isClosed())
            throw new IllegalStateException("A downloaded skin was registered without pixels");

        SKINS.put(texture.getTextureView(), new SkinAlpha(pixels));
    }

    public static void forget(@Nullable GpuTextureView view) {
        if (view != null)
            SKINS.remove(view);
    }

    public static @Nullable SkinAlpha of(PreparedRenderType prepared) {
        if (!CUTOUTS.computeIfAbsent(prepared.pipeline(), SkinAlpha::discardsTransparent))
            return null;

        for (PreparedRenderType.Texture texture : prepared.textures()) {
            if (texture.name().equals(SAMPLER))
                return SKINS.get(texture.textureView());
        }

        return null;
    }

    public static int allFaces(ModelPart.Cube cube) {
        return (1 << cube.polygons.length) - 1;
    }

    public int hiddenFaces(ModelPart.Cube cube) {
        Answer answer = this.hidden.get(cube);

        if (answer == null || !answer.matches(cube.polygons)) {
            answer = new Answer(cube.polygons.clone(), this.transparentFaces(cube.polygons));
            this.hidden.put(cube, answer);
        }

        return answer.faces();
    }

    private int transparentFaces(ModelPart.Polygon[] polygons) {
        if (polygons.length >= Integer.SIZE)
            throw new IllegalArgumentException("A cube with " + polygons.length + " faces does not fit a face mask");

        int faces = 0;

        for (int face = 0; face < polygons.length; face ++) {
            if (this.transparent(polygons[face]))
                faces |= 1 << face;
        }

        return faces;
    }

    private boolean transparent(ModelPart.Polygon polygon) {
        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;

        for (ModelPart.Vertex vertex : polygon.vertices()) {
            minU = Math.min(minU, vertex.u());
            minV = Math.min(minV, vertex.v());
            maxU = Math.max(maxU, vertex.u());
            maxV = Math.max(maxV, vertex.v());
        }

        int x0 = (int) Math.floor(minU * this.width) - MARGIN;
        int y0 = (int) Math.floor(minV * this.height) - MARGIN;
        int x1 = (int) Math.ceil(maxU * this.width) + MARGIN;
        int y1 = (int) Math.ceil(maxV * this.height) + MARGIN;

        if (x0 < 0 || y0 < 0 || x1 > this.width || y1 > this.height)
            return false;

        for (int y = y0; y < y1; y ++) {
            for (int x = x0; x < x1; x ++) {
                int texel = y * this.width + x;

                if ((this.visible[texel / Long.SIZE] & 1L << texel % Long.SIZE) != 0L)
                    return false;
            }
        }

        return true;
    }

    private record Answer(ModelPart.Polygon[] polygons, int faces) {

        private boolean matches(ModelPart.Polygon[] current) {
            if (current.length != this.polygons.length)
                return false;

            for (int index = 0; index < current.length; index ++) {
                if (current[index] != this.polygons[index])
                    return false;
            }

            return true;
        }
    }

    private static boolean discardsTransparent(RenderPipeline pipeline) {
        String cutout = pipeline.getShaderDefines().values().get(CUTOUT);

        return cutout != null && Float.parseFloat(cutout) > 0.0F;
    }
}
