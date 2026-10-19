package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.OutlineModelView;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PreparedRenderType.class)
public abstract class PreparedRenderTypeModelViewMixin implements OutlineModelView {

    @Unique
    private Matrix4fc txoptimizations$modelView;

    @Override
    public Matrix4fc txoptimizations$modelView() {
        return this.txoptimizations$modelView;
    }

    @Override
    public void txoptimizations$setModelView(Matrix4fc modelView) {
        this.txoptimizations$modelView = modelView;
    }
}
