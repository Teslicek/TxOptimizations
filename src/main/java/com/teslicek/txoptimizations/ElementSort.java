package com.teslicek.txoptimizations;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public final class ElementSort {

    public static final Comparator<GuiElementRenderState> ORDER = (first, second) -> {
        throw new IllegalStateException("GUI elements in this order are only sorted by ElementSort");
    };

    private static final int KEYS             = 8;
    private static final int INSERTION_LENGTH = 16;

    private static GuiElementRenderState[] elements = new GuiElementRenderState[64];
    private static int[]                   keys     = new int[64 * KEYS];
    private static int[]                   order    = new int[64];
    private static int[]                   merged   = new int[64];

    private ElementSort() {
    }

    public static void sort(List<GuiElementRenderState> list) {
        int count = list.size();

        if (count < 2)
            return;

        reserve(count);

        for (int index = 0; index < count; index ++) {
            GuiElementRenderState element = list.get(index);

            elements[index] = element;
            order[index]    = index;
            fill(index * KEYS, element);
        }

        sort(0, count);

        for (int index = 0; index < count; index ++)
            list.set(index, elements[order[index]]);

        Arrays.fill(elements, 0, count, null);
    }

    private static void fill(int at, GuiElementRenderState element) {
        ScreenRectangle scissor = element.scissorArea();
        TextureSetup    texture = element.textureSetup();

        keys[at]     = scissor == null ? 0 : 1;
        keys[at + 1] = scissor == null ? 0 : scissor.top();
        keys[at + 2] = scissor == null ? 0 : scissor.bottom();
        keys[at + 3] = scissor == null ? 0 : scissor.left();
        keys[at + 4] = scissor == null ? 0 : scissor.right();
        keys[at + 5] = element.pipeline().getSortKey();
        keys[at + 6] = texture == null ? 0 : 1;
        keys[at + 7] = texture == null ? 0 : texture.getSortKey();
    }

    private static void sort(int from, int to) {
        if (to - from <= INSERTION_LENGTH) {
            insertion(from, to);
            return;
        }

        int middle = from + to >>> 1;

        sort(from, middle);
        sort(middle, to);

        if (compare(order[middle - 1], order[middle]) <= 0)
            return;

        int left  = from;
        int right = middle;
        int at    = from;

        while (left < middle && right < to)
            merged[at ++] = compare(order[right], order[left]) < 0 ? order[right ++] : order[left ++];

        while (left < middle)
            merged[at ++] = order[left ++];

        while (right < to)
            merged[at ++] = order[right ++];

        System.arraycopy(merged, from, order, from, to - from);
    }

    private static void insertion(int from, int to) {
        for (int index = from + 1; index < to; index ++) {
            int element = order[index];
            int at      = index - 1;

            while (at >= from && compare(order[at], element) > 0) {
                order[at + 1] = order[at];
                at --;
            }

            order[at + 1] = element;
        }
    }

    private static int compare(int first, int second) {
        int a = first * KEYS;
        int b = second * KEYS;

        for (int key = 0; key < KEYS; key ++) {
            int difference = Integer.compare(keys[a + key], keys[b + key]);

            if (difference != 0)
                return difference;
        }

        return 0;
    }

    private static void reserve(int count) {
        if (count <= order.length)
            return;

        int capacity = Integer.highestOneBit(count - 1) << 1;

        elements = new GuiElementRenderState[capacity];
        keys     = new int[capacity * KEYS];
        order    = new int[capacity];
        merged   = new int[capacity];
    }
}
