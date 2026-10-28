package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.RenderThreadStack;
import com.teslicek.txoptimizations.TextContentCache;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionTextPoseMixin {

    @Unique
    private @Nullable Matrix4f txoptimizations$lastTextPose;

    @Redirect(method = "submitTextPart", at = @At(value = "NEW", target = "(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"))
    private Matrix4f txoptimizations$shareTextPose(Matrix4fc pose) {
        Matrix4f last = this.txoptimizations$lastTextPose;

        if (last != null && txoptimizations$sameBits(last, pose))
            return last;

        Matrix4f copy = new Matrix4f(pose);

        this.txoptimizations$lastTextPose = copy;

        return copy;
    }

    @Redirect(method = "submitText", at = @At(value = "NEW", target = "(FFLnet/minecraft/util/FormattedCharSequence;ZIII)Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Content$Text;"))
    private TextFeatureRenderer.Content.Text txoptimizations$reuseText(float x, float y, FormattedCharSequence string, boolean dropShadow, int color, int backgroundColor, int outlineColor) {
        if (!RenderThreadStack.isOwner())
            return new TextFeatureRenderer.Content.Text(x, y, string, dropShadow, color, backgroundColor, outlineColor);

        return TextContentCache.text(x, y, string, dropShadow, color, backgroundColor, outlineColor);
    }

    @Unique
    private static boolean txoptimizations$sameBits(Matrix4fc first, Matrix4fc second) {
        return first.properties() == second.properties()
            && (Float.floatToRawIntBits(first.m00()) ^ Float.floatToRawIntBits(second.m00()) | Float.floatToRawIntBits(first.m01()) ^ Float.floatToRawIntBits(second.m01()) | Float.floatToRawIntBits(first.m02()) ^ Float.floatToRawIntBits(second.m02()) | Float.floatToRawIntBits(first.m03()) ^ Float.floatToRawIntBits(second.m03())
            | Float.floatToRawIntBits(first.m10()) ^ Float.floatToRawIntBits(second.m10()) | Float.floatToRawIntBits(first.m11()) ^ Float.floatToRawIntBits(second.m11()) | Float.floatToRawIntBits(first.m12()) ^ Float.floatToRawIntBits(second.m12()) | Float.floatToRawIntBits(first.m13()) ^ Float.floatToRawIntBits(second.m13())
            | Float.floatToRawIntBits(first.m20()) ^ Float.floatToRawIntBits(second.m20()) | Float.floatToRawIntBits(first.m21()) ^ Float.floatToRawIntBits(second.m21()) | Float.floatToRawIntBits(first.m22()) ^ Float.floatToRawIntBits(second.m22()) | Float.floatToRawIntBits(first.m23()) ^ Float.floatToRawIntBits(second.m23())
            | Float.floatToRawIntBits(first.m30()) ^ Float.floatToRawIntBits(second.m30()) | Float.floatToRawIntBits(first.m31()) ^ Float.floatToRawIntBits(second.m31()) | Float.floatToRawIntBits(first.m32()) ^ Float.floatToRawIntBits(second.m32()) | Float.floatToRawIntBits(first.m33()) ^ Float.floatToRawIntBits(second.m33())) == 0;
    }
}
