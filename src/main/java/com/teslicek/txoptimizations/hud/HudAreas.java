package com.teslicek.txoptimizations.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;

final class HudAreas {

    private static final int MAX_AREAS = 32;
    private static final int PADDING   = 2;

    private final int[] left   = new int[MAX_AREAS];
    private final int[] top    = new int[MAX_AREAS];
    private final int[] right  = new int[MAX_AREAS];
    private final int[] bottom = new int[MAX_AREAS];

    private int count;

    void clear() {
        this.count = 0;
    }

    void include(int areaLeft, int areaTop, int areaRight, int areaBottom) {
        int paddedLeft   = areaLeft - PADDING;
        int paddedTop    = areaTop - PADDING;
        int paddedRight  = areaRight + PADDING;
        int paddedBottom = areaBottom + PADDING;

        if (this.count < MAX_AREAS) {
            int index = this.count ++;

            this.left[index]   = paddedLeft;
            this.top[index]    = paddedTop;
            this.right[index]  = paddedRight;
            this.bottom[index] = paddedBottom;
            this.mergeOverlapping(index);

            return;
        }

        int index = this.cheapestMerge(paddedLeft, paddedTop, paddedRight, paddedBottom);

        this.left[index]   = Math.min(this.left[index], paddedLeft);
        this.top[index]    = Math.min(this.top[index], paddedTop);
        this.right[index]  = Math.max(this.right[index], paddedRight);
        this.bottom[index] = Math.max(this.bottom[index], paddedBottom);
        this.mergeOverlapping(index);
    }

    List<ScreenRectangle> rectangles(int width, int height) {
        List<ScreenRectangle> rectangles = new ArrayList<>(this.count);

        for (int index = 0; index < this.count; index ++) {
            int areaLeft   = Math.max(this.left[index], 0);
            int areaTop    = Math.max(this.top[index], 0);
            int areaRight  = Math.min(this.right[index], width);
            int areaBottom = Math.min(this.bottom[index], height);

            if (areaRight > areaLeft && areaBottom > areaTop)
                rectangles.add(new ScreenRectangle(areaLeft, areaTop, areaRight - areaLeft, areaBottom - areaTop));
        }

        return rectangles;
    }

    private int cheapestMerge(int areaLeft, int areaTop, int areaRight, int areaBottom) {
        int  cheapest = -1;
        long growth   = Long.MAX_VALUE;

        for (int index = 0; index < this.count; index ++) {
            long merged = (long) (Math.max(this.right[index], areaRight) - Math.min(this.left[index], areaLeft)) * (Math.max(this.bottom[index], areaBottom) - Math.min(this.top[index], areaTop));
            long added  = merged - (long) (this.right[index] - this.left[index]) * (this.bottom[index] - this.top[index]);

            if (added < growth) {
                growth   = added;
                cheapest = index;
            }
        }

        return cheapest;
    }

    private void mergeOverlapping(int target) {
        int index = 0;

        while (index < this.count) {
            if (index == target || !this.overlaps(index, target)) {
                index ++;
                continue;
            }

            this.left[target]   = Math.min(this.left[target], this.left[index]);
            this.top[target]    = Math.min(this.top[target], this.top[index]);
            this.right[target]  = Math.max(this.right[target], this.right[index]);
            this.bottom[target] = Math.max(this.bottom[target], this.bottom[index]);

            int last = this.count - 1;

            this.left[index]   = this.left[last];
            this.top[index]    = this.top[last];
            this.right[index]  = this.right[last];
            this.bottom[index] = this.bottom[last];
            this.count --;

            if (target == last)
                target = index;

            index = 0;
        }
    }

    private boolean overlaps(int first, int second) {
        return this.left[first] < this.right[second] && this.left[second] < this.right[first] && this.top[first] < this.bottom[second] && this.top[second] < this.bottom[first];
    }
}
