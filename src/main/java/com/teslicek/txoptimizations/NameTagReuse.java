package com.teslicek.txoptimizations;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class NameTagReuse {

    private NameTagReuse() {
    }

    public static boolean matches(Component remembered, Component fresh) {
        return remembered != null && remembered.equals(fresh);
    }

    public static Component remembered(Component fresh) {
        if (fresh == null || !isStatic(fresh))
            return null;

        return fresh;
    }

    private static boolean isStatic(Component component) {
        ComponentContents contents = component.getContents();

        if (contents instanceof TranslatableContents translatable) {
            for (Object argument : translatable.getArgs())
                if (argument instanceof Component nested && !isStatic(nested))
                    return false;
        } else if (!(contents instanceof PlainTextContents)) {
            return false;
        }

        for (Component sibling : component.getSiblings())
            if (!isStatic(sibling))
                return false;

        return true;
    }
}
