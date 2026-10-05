package com.teslicek.txoptimizations.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.font.FontManager$Preparation")
public interface FontManagerPreparationAccessor {

    @Accessor("fontSets")
    Map<Identifier, List<GlyphProvider.Conditional>> txoptimizations$fontSets();
}
