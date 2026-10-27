package com.teslicek.txoptimizations;

import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.Display;

public final class TextCullingBox {

    private final Font                      font;
    private final int                       generation;
    private final int                       backgroundColor;
    private final int                       width;
    private final int                       height;
    private final boolean                   shadow;
    private final Display.TextDisplay.Align alignment;
    private final int                       color;
    private final float                     minX;
    private final float                     minY;
    private final float                     maxX;
    private final float                     maxY;

    public TextCullingBox(Font font, int generation, int backgroundColor, int width, int height, boolean shadow, Display.TextDisplay.Align alignment, int color, float minX, float minY, float maxX, float maxY) {
        this.font            = font;
        this.generation      = generation;
        this.backgroundColor = backgroundColor;
        this.width           = width;
        this.height          = height;
        this.shadow          = shadow;
        this.alignment       = alignment;
        this.color           = color;
        this.minX            = minX;
        this.minY            = minY;
        this.maxX            = maxX;
        this.maxY            = maxY;
    }

    public boolean isFor(Font font, int generation, int backgroundColor, int width, int height, boolean shadow, Display.TextDisplay.Align alignment, int color) {
        return this.font == font && this.generation == generation && this.backgroundColor == backgroundColor && this.width == width && this.height == height && this.shadow == shadow && this.alignment == alignment && this.color == color;
    }

    public float minX() {
        return this.minX;
    }

    public float minY() {
        return this.minY;
    }

    public float maxX() {
        return this.maxX;
    }

    public float maxY() {
        return this.maxY;
    }
}
