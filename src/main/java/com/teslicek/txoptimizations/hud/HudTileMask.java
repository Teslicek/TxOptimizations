package com.teslicek.txoptimizations.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.teslicek.txoptimizations.RenderWorker;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

final class HudTileMask {

    private static final int              TILE       = 32;
    private static final int              TILE_BYTES = 4;
    private static final int              CORNERS    = 3;
    private static final Supplier<String> LABEL      = () -> "TxOptimizations HUD tiles";
    private static final RenderPipeline   PIPELINE   = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("txoptimizations", "pipeline/hud_tiles"))
        .withVertexShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/hud_tiles"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("txoptimizations", "shaderc/hud_tiles"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("Hud", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .build();

    private final HudAreas     areas  = new HudAreas();
    private final RenderTarget target = new TextureTarget("txoptimizations_hud_tiles", 1, 1, GpuFormat.RGBA8_UNORM, null);

    private @Nullable GpuBuffer     readback;
    private @Nullable GpuBuffer     reading;
    private volatile @Nullable Mask published;
    private int                     columns;
    private int                     rows;
    private int                     width;
    private int                     height;
    private int                     guiScale;
    private int                     guiWidth;
    private int                     guiHeight;

    void resize(Window window) {
        this.width     = window.getWidth();
        this.height    = window.getHeight();
        this.guiScale  = window.getGuiScale();
        this.guiWidth  = window.getGuiScaledWidth();
        this.guiHeight = window.getGuiScaledHeight();
        this.columns   = Math.ceilDiv(this.width, TILE);
        this.rows      = Math.ceilDiv(this.height, TILE);
        this.published = null;
        this.target.resize(this.columns, this.rows);

        if (this.readback != null && this.readback != this.reading)
            this.readback.close();

        this.readback = RenderSystem.getDevice().createBuffer(LABEL, GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, (long) this.columns * this.rows * TILE_BYTES);
    }

    @Nullable HudBlitAreas areas(int serial) {
        Mask mask = this.published;

        if (mask == null || mask.serial != serial)
            return null;

        return mask.areas;
    }

    void render(RenderTarget source, int serial) {
        if (!RenderWorker.isCurrent())
            throw new IllegalStateException("HUD tiles were rendered outside the render worker");

        if (!HudShaderOverrides.isMoved() || this.reading == this.readback)
            return;

        GpuBuffer      buffer  = this.readback;
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        if (buffer == null)
            throw new IllegalStateException("HUD tiles were rendered before the first resize");

        try (RenderPass pass = encoder.createRenderPass(LABEL, this.target.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
            pass.setUniform("Hud", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(CORNERS, 1, 0, 0);
        }

        this.reading = buffer;
        encoder.copyTextureToBuffer(this.target.getColorTexture(), buffer, 0L, () -> this.publish(buffer, serial), 0);
    }

    private void publish(GpuBuffer buffer, int serial) {
        if (this.reading == buffer)
            this.reading = null;

        if (buffer != this.readback) {
            buffer.close();
            return;
        }

        this.areas.clear();

        try (GpuBufferSlice.MappedView view = buffer.map(true, false)) {
            ByteBuffer data = view.data();

            for (int row = 0; row < this.rows; row ++)
                this.includeRow(data, row);
        }

        this.published = new Mask(serial, HudBlitAreas.of(this.areas.rectangles(this.guiWidth, this.guiHeight)));
    }

    private void includeRow(ByteBuffer data, int row) {
        int top    = (this.height - Math.min((row + 1) * TILE, this.height)) / this.guiScale;
        int bottom = Math.ceilDiv(this.height - row * TILE, this.guiScale);
        int first  = -1;

        for (int column = 0; column <= this.columns; column ++) {
            boolean filled = column < this.columns && data.get((row * this.columns + column) * TILE_BYTES) != 0;

            if (filled && first < 0)
                first = column;

            if (filled || first < 0)
                continue;

            this.areas.include(first * TILE / this.guiScale, top, Math.ceilDiv(Math.min(column * TILE, this.width), this.guiScale), bottom);
            first = -1;
        }
    }

    private record Mask(int serial, HudBlitAreas areas) {
    }
}
