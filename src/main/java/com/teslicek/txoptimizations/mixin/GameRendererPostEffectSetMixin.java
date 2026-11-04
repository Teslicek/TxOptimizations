package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.EmptyHashSet;
import java.util.Collection;
import java.util.HashSet;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererPostEffectSetMixin {

    @Redirect(method = "preparePostEffects", at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/HashSet;"), require = 1)
    private HashSet<Object> txoptimizations$previousEffects(Collection<Object> applied) {
        if (applied.isEmpty())
            return EmptyHashSet.instance();

        return new HashSet<>(applied);
    }
}
