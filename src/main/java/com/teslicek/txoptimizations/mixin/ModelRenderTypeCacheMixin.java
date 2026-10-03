package com.teslicek.txoptimizations.mixin;

import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Model.class)
public abstract class ModelRenderTypeCacheMixin {

    @Unique
    private Last txoptimizations$last;

    @Shadow
    public abstract Function<Identifier, RenderType> renderType();

    @Overwrite
    public final RenderType renderType(Identifier texture) {
        Last last = this.txoptimizations$last;

        if (last != null && last.texture() == texture)
            return last.type();

        RenderType type = this.renderType().apply(texture);

        this.txoptimizations$last = new Last(texture, type);

        return type;
    }

    @Unique
    private record Last(Identifier texture, RenderType type) {
    }
}
