package com.teslicek.txoptimizations.hud;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;

record HudBlitAreas(List<ScreenRectangle> areas, ScreenRectangle bounds) {

    static final HudBlitAreas EMPTY = new HudBlitAreas(List.of(), ScreenRectangle.empty());

    static HudBlitAreas of(List<ScreenRectangle> areas) {
        if (areas.isEmpty())
            return EMPTY;

        int left   = Integer.MAX_VALUE;
        int top    = Integer.MAX_VALUE;
        int right  = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;

        for (ScreenRectangle area : areas) {
            left   = Math.min(left, area.left());
            top    = Math.min(top, area.top());
            right  = Math.max(right, area.right());
            bottom = Math.max(bottom, area.bottom());
        }

        return new HudBlitAreas(areas, new ScreenRectangle(left, top, right - left, bottom - top));
    }
}
