package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GuiLayerBounds;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.client.renderer.state.gui.GuiRenderState$Node")
public abstract class GuiRenderStateNodeBoundsMixin implements GuiLayerBounds {

    @Unique
    private static final int[] NO_BOUNDS = new int[0];

    @Shadow
    public List<GuiElementRenderState> elementStates;

    @Shadow
    public List<GuiItemRenderState> itemStates;

    @Shadow
    public List<GuiTextRenderState> textStates;

    @Shadow
    public List<PictureInPictureRenderState> picturesInPictureStates;

    @Unique
    private int[] txoptimizations$bounds = NO_BOUNDS;

    @Unique
    private int txoptimizations$boundsLength;

    @Unique
    private List<?> txoptimizations$recordedElements;

    @Unique
    private List<?> txoptimizations$recordedItems;

    @Unique
    private List<?> txoptimizations$recordedTexts;

    @Unique
    private List<?> txoptimizations$recordedPictures;

    @Unique
    private int txoptimizations$elementCount;

    @Unique
    private int txoptimizations$itemCount;

    @Unique
    private int txoptimizations$textCount;

    @Unique
    private int txoptimizations$pictureCount;

    @Override
    public void txoptimizations$recordNewBounds() {
        if (!this.txoptimizations$listsOnlyGrew()) {
            this.txoptimizations$boundsLength = 0;
            this.txoptimizations$elementCount = 0;
            this.txoptimizations$itemCount    = 0;
            this.txoptimizations$textCount    = 0;
            this.txoptimizations$pictureCount = 0;
        }

        this.txoptimizations$elementCount     = this.txoptimizations$record(this.elementStates, this.txoptimizations$elementCount);
        this.txoptimizations$itemCount        = this.txoptimizations$record(this.itemStates, this.txoptimizations$itemCount);
        this.txoptimizations$textCount        = this.txoptimizations$record(this.textStates, this.txoptimizations$textCount);
        this.txoptimizations$pictureCount     = this.txoptimizations$record(this.picturesInPictureStates, this.txoptimizations$pictureCount);
        this.txoptimizations$recordedElements = this.elementStates;
        this.txoptimizations$recordedItems    = this.itemStates;
        this.txoptimizations$recordedTexts    = this.textStates;
        this.txoptimizations$recordedPictures = this.picturesInPictureStates;
    }

    @Override
    public boolean txoptimizations$intersects(int left, int top, int right, int bottom) {
        int[] bounds = this.txoptimizations$bounds;
        int   length = this.txoptimizations$boundsLength;

        for (int i = 0; i < length; i += 4) {
            if (bounds[i] < right && bounds[i + 2] > left && bounds[i + 1] < bottom && bounds[i + 3] > top)
                return true;
        }

        return false;
    }

    @Unique
    private boolean txoptimizations$listsOnlyGrew() {
        return txoptimizations$onlyGrew(this.elementStates, this.txoptimizations$recordedElements, this.txoptimizations$elementCount) && txoptimizations$onlyGrew(this.itemStates, this.txoptimizations$recordedItems, this.txoptimizations$itemCount) && txoptimizations$onlyGrew(this.textStates, this.txoptimizations$recordedTexts, this.txoptimizations$textCount) && txoptimizations$onlyGrew(this.picturesInPictureStates, this.txoptimizations$recordedPictures, this.txoptimizations$pictureCount);
    }

    @Unique
    private int txoptimizations$record(List<? extends ScreenArea> states, int from) {
        if (states == null)
            return 0;

        int size = states.size();

        for (int i = from; i < size; i ++) {
            ScreenRectangle area = states.get(i).bounds();

            if (area != null)
                this.txoptimizations$append(area);
        }

        return size;
    }

    @Unique
    private void txoptimizations$append(ScreenRectangle area) {
        int length = this.txoptimizations$boundsLength;

        if (length == this.txoptimizations$bounds.length)
            this.txoptimizations$bounds = Arrays.copyOf(this.txoptimizations$bounds, Math.max(16, length * 2));

        int[] bounds = this.txoptimizations$bounds;

        bounds[length]                    = area.left();
        bounds[length + 1]                = area.top();
        bounds[length + 2]                = area.right();
        bounds[length + 3]                = area.bottom();
        this.txoptimizations$boundsLength = length + 4;
    }

    @Unique
    private static boolean txoptimizations$onlyGrew(List<?> states, List<?> recorded, int count) {
        return count == 0 || states == recorded && states.size() >= count;
    }
}
