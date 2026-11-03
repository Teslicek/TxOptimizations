package com.teslicek.txoptimizations;

import net.minecraft.client.gui.Font;
import org.jspecify.annotations.Nullable;

public interface TextLayouts {

    Font.@Nullable PreparedText txoptimizations$layout(Font font, int generation, boolean outline);

    void txoptimizations$setLayout(Font font, int generation, boolean outline, Font.PreparedText layout);
}
